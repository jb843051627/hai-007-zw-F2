package com.fc.v2.service.zw;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TZwLimRule;

/**
 * 水质读数分档与选线口径——全站只能有这一把尺。
 *
 * <p>所有裁决（读数合不合法、某刻哪条线现行、并线听谁的、空界值怎么处置、落在哪一档）
 * 都只在此处定夺，服务层那个评价出口与历史复核一律调这里，页面不得自己先算一档。
 *
 * <p><b>现行可用</b>：未删除、情形为现行（status=0，让了位的线只作回溯依据）、
 * 检测时刻 at 落在 [eff_start, eff_end)（启用之日含、交棒之日不含）。
 * 用哪条线只看检测发生那天，不看今天。
 *
 * <p><b>选线</b>：只在同检测项、同样本类别的并行使权线之间排——顺位（priority）数值大的先说；
 * 顺位分不出，按代号序列排后的一条胜出（ruleCode 降序）。次序固定、四季一致，
 * 不许今秋选前、明春选后。
 *
 * <p><b>分档</b>：读数与界值相同算高档（恰为 th1 入关注、恰为 th2 入超标、恰为 th3 入严重超标）。
 * 界值空 = 不设此档，不算断档、不报错：越过已设最高界值的读数随该档继续下推
 * （如 th3 空，则再大也只到超标档，不升严重超标）。三档界值全空判不了，返回 0。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class WqGradeRule {

    /** 合格 */
    public static final int GRADE_OK = 1;
    /** 关注 */
    public static final int GRADE_WATCH = 2;
    /** 超标 */
    public static final int GRADE_OVER = 3;
    /** 严重超标 */
    public static final int GRADE_SEVERE = 4;
    /** 判不了（无可用线 / 三档全空 / 读数非法）——不是档位，调用方不得当档用 */
    public static final int GRADE_NONE = 0;

    /** 判定线情形 0现行 1已让位 */
    public static final int STATUS_CURRENT = 0;

    /** 样本类别三管各管各的 */
    public static final String CAT_SOURCE = "水源水";
    public static final String CAT_FINISHED = "出厂水";
    public static final String CAT_TAP = "管网末梢";

    /** 合理读数域：负数与超大怪值都退回重录；列域 decimal(12,2)，上限取 100 */
    public static final BigDecimal QTY_MIN = BigDecimal.ZERO;
    public static final BigDecimal QTY_MAX = new BigDecimal("100");

    /** 顺位降序；并列按代号降序（排后的一条胜出），确定性、可复现 */
    public static final Comparator<TZwLimRule> PRIORITY_THEN_CODE_DESC = new Comparator<TZwLimRule>() {
        @Override
        public int compare(TZwLimRule a, TZwLimRule b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority().intValue();
            int pb = b.getPriority() == null ? 0 : b.getPriority().intValue();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    private WqGradeRule() {
    }

    /**
     * 读数是否在合理区间。空值、负数、超大怪值一律不参与定级，退回化验员重录。
     */
    public static boolean validReading(BigDecimal qty) {
        return qty != null
                && qty.compareTo(QTY_MIN) >= 0
                && qty.compareTo(QTY_MAX) <= 0;
    }

    /**
     * 唯一的现行可用谓词：未删除、旗为现行、at 落在 [eff_start, eff_end)。
     * 起止皆空=长期现行；只空一头=单侧开放。比较按墙钟毫秒在内存里做，
     * 不把 java.util.Date 绑进 SQL（serverTimezone 不对称时交接日边界会整体错档）。
     */
    public static boolean effective(TZwLimRule r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag().intValue() != 0) {
            return false;
        }
        if (r.getStatus() == null || r.getStatus().intValue() != STATUS_CURRENT) {
            return false;
        }
        long t = at.getTime();
        if (r.getEffStart() != null && t < r.getEffStart().getTime()) {
            return false;
        }
        // 交棒之日不含：恰在交棒日 00:00:00 已归新线口径
        if (r.getEffEnd() != null && t >= r.getEffEnd().getTime()) {
            return false;
        }
        return true;
    }

    /** 是否管这个检测项+样本类别（并线只在同项同类之间排） */
    public static boolean applies(TZwLimRule r, String itemCode, String sampleCategory) {
        return r != null
                && itemCode != null && itemCode.equals(r.getItemCode())
                && sampleCategory != null && sampleCategory.equals(r.getSampleCategory());
    }

    /**
     * 从 at 时刻并行使权的线里选说了算的一条：同项同类 + 现行可用，
     * 顺位高者胜，并列取代号排后者。候选项外的返回 null（调用方须写「无可用标准」，不得凑合）。
     */
    public static TZwLimRule pick(List<TZwLimRule> rules, String itemCode, String sampleCategory, Date at) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        TZwLimRule picked = null;
        for (TZwLimRule r : rules) {
            if (!applies(r, itemCode, sampleCategory) || !effective(r, at)) {
                continue;
            }
            if (picked == null || PRIORITY_THEN_CODE_DESC.compare(r, picked) < 0) {
                picked = r;
            }
        }
        return picked;
    }

    /** 列表按选线次序排好（首位即说了算的那条），便于台账与评价同口径 */
    public static void sortByPriority(List<TZwLimRule> rules) {
        if (rules != null) {
            Collections.sort(rules, PRIORITY_THEN_CODE_DESC);
        }
    }

    /**
     * 读数套进三个界值定档。等于界值算高档（只有严格小于界值才留在本档）；
     * 空界值=不设此档，跳过该档，越过下一个已设界值直接升到该档，档名固定不补位；
     * 最高档界值空时，读数再大也停在已设的最高档（如 th3 空则永不升严重超标，随超标档下推）。
     * 三档界值全空的早期残线判不了，返回 {@link #GRADE_NONE}。
     */
    public static int grade(TZwLimRule r, BigDecimal qty) {
        if (r == null || qty == null) {
            return GRADE_NONE;
        }
        BigDecimal th1 = r.getTh1Max();
        BigDecimal th2 = r.getTh2Max();
        BigDecimal th3 = r.getTh3Max();
        if (th1 == null && th2 == null && th3 == null) {
            return GRADE_NONE;
        }
        BigDecimal[] thresholds = new BigDecimal[] {th1, th2, th3};
        int grade = GRADE_OK;
        for (int i = 0; i < thresholds.length; i++) {
            if (thresholds[i] == null) {
                // 这一档不设：不升档、不补位，继续看下一条已设界值
                continue;
            }
            if (qty.compareTo(thresholds[i]) < 0) {
                return grade;
            }
            // 恰为界值也算越过：升到第 i+2 档（档名固定，不因中间缺档而占空名）
            grade = i + 2;
        }
        // 最高档界值空：越线再多也停在已设最高档，不硬升
        return grade;
    }

    /** 化验员按等级怎么办：严重超标立即上报复核，超标安排复测，关注并入下次检测 */
    public static String followAction(int grade) {
        switch (grade) {
            case GRADE_SEVERE:
                return "立即上报复核";
            case GRADE_OVER:
                return "安排复测";
            case GRADE_WATCH:
                return "并入下次检测";
            case GRADE_OK:
                return "合格，无需处置";
            default:
                return "无结论";
        }
    }
}
