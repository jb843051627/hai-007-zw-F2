package com.fc.v2.service.zw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TZwLimRule;

/**
 * 水质读数分档与选线唯一尺子的口径测试：
 * 等于界值算高档、空界值不设档下推、交接日含起不含终、并线顺位与代号决胜、
 * 样本类别各管各的、无可用标准返回空、读数合理区间。
 */
public class WqGradeRuleTest {

    private static final String FINISHED = "出厂水";
    private static final String SOURCE = "水源水";
    private static final String TAP = "管网末梢";

    private static Date ts(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
    }

    private static TZwLimRule rule(String code, String item, String category,
                                   String th1, String th2, String th3,
                                   String start, String end, Integer priority, Integer status) {
        TZwLimRule r = new TZwLimRule();
        r.setRuleCode(code);
        r.setItemCode(item);
        r.setSampleCategory(category);
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        try {
            r.setEffStart(start == null ? null : ts(start));
            r.setEffEnd(end == null ? null : ts(end));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(0);
        return r;
    }

    // ── 读数合理区间：负数、超大怪值、空值退回重录 ──

    @Test
    public void readingRange() {
        assertTrue(WqGradeRule.validReading(new BigDecimal("0")));
        assertTrue(WqGradeRule.validReading(new BigDecimal("0.05")));
        assertTrue(WqGradeRule.validReading(new BigDecimal("100")));
        assertFalse(WqGradeRule.validReading(null));
        assertFalse(WqGradeRule.validReading(new BigDecimal("-0.01")));
        assertFalse(WqGradeRule.validReading(new BigDecimal("100.01")));
        assertFalse(WqGradeRule.validReading(new BigDecimal("999999")));
    }

    // ── 等于界值算高档（故事：恰值旧线中度上限，旧规矩填低了一档，点位拖到次月） ──

    @Test
    public void equalThresholdTakesHigherGrade() {
        TZwLimRule r = rule("X", "CL", FINISHED, "0.30", "0.50", "1.00",
                "2025-01-01 00:00:00", null, 10, 0);
        assertEquals(1, WqGradeRule.grade(r, new BigDecimal("0.29")));
        assertEquals(2, WqGradeRule.grade(r, new BigDecimal("0.30")));
        assertEquals(2, WqGradeRule.grade(r, new BigDecimal("0.49")));
        assertEquals(3, WqGradeRule.grade(r, new BigDecimal("0.50")));
        assertEquals(3, WqGradeRule.grade(r, new BigDecimal("0.99")));
        assertEquals(4, WqGradeRule.grade(r, new BigDecimal("1.00")));
        assertEquals(4, WqGradeRule.grade(r, new BigDecimal("1.20")));
    }

    // ── 早期入册线严重超标格空：空即不设此档，再大也停在超标档，不报错、不硬升 ──

    @Test
    public void nullTh3MeansNoSevereGrade() {
        TZwLimRule r = rule("TURB-SRC-E", "TURB", SOURCE, "10", "20", null,
                "2025-01-01 00:00:00", null, 10, 0);
        assertEquals(1, WqGradeRule.grade(r, new BigDecimal("9")));
        assertEquals(2, WqGradeRule.grade(r, new BigDecimal("10")));
        assertEquals(2, WqGradeRule.grade(r, new BigDecimal("19.99")));
        assertEquals(3, WqGradeRule.grade(r, new BigDecimal("20")));
        // 越过已设最高界值，随超标档继续下推——永不升严重超标
        assertEquals(3, WqGradeRule.grade(r, new BigDecimal("999")));
    }

    // ── 中间/低档界值空：档名固定不补位，跳过空档直接到下一已设档 ──

    @Test
    public void nullLowerThresholdsSkipThatGrade() {
        // 关注档不设（th1 空）：小于超标上限一律合格
        TZwLimRule noWatch = rule("A", "X", SOURCE, null, "0.50", "1.00",
                "2025-01-01 00:00:00", null, 10, 0);
        assertEquals(1, WqGradeRule.grade(noWatch, new BigDecimal("0.49")));
        assertEquals(3, WqGradeRule.grade(noWatch, new BigDecimal("0.50")));
        assertEquals(4, WqGradeRule.grade(noWatch, new BigDecimal("1.00")));

        // 超标档不设（th2 空）：关注之上、严重之下不补"超标"的名，越过严重上限直接严重超标
        TZwLimRule noOver = rule("B", "X", SOURCE, "0.30", null, "1.00",
                "2025-01-01 00:00:00", null, 10, 0);
        assertEquals(1, WqGradeRule.grade(noOver, new BigDecimal("0.20")));
        assertEquals(2, WqGradeRule.grade(noOver, new BigDecimal("0.30")));
        assertEquals(2, WqGradeRule.grade(noOver, new BigDecimal("0.99")));
        assertEquals(4, WqGradeRule.grade(noOver, new BigDecimal("1.00")));

        // 三档全空：判不了，不是合格
        TZwLimRule allNull = rule("C", "X", SOURCE, null, null, null,
                "2025-01-01 00:00:00", null, 10, 0);
        assertEquals(WqGradeRule.GRADE_NONE, WqGradeRule.grade(allNull, new BigDecimal("0.5")));
    }

    // ── 交接那一天全厂一套口径：旧线管到交棒前一秒，新线从交棒日 00:00:00 起算 ──

    @Test
    public void handoverDaySingleStandard() throws Exception {
        TZwLimRule oldCurrent = rule("CL-OUT-2025", "CL", FINISHED, "0.30", "0.50", "1.00",
                "2025-01-01 00:00:00", "2026-01-01 00:00:00", 10, 0);
        TZwLimRule newLine = rule("CL-OUT-2026", "CL", FINISHED, "0.20", "0.40", "0.80",
                "2026-01-01 00:00:00", null, 10, 0);
        Date lastSecond = ts("2025-12-31 23:59:59");
        Date handover = ts("2026-01-01 00:00:00");
        List<TZwLimRule> both = Arrays.asList(oldCurrent, newLine);
        // 交接那天全厂一套口径：交棒前一秒仍归旧线，交棒日 00:00:00 起整厂只听新线
        assertSame(oldCurrent, WqGradeRule.pick(both, "CL", FINISHED, lastSecond));
        assertSame(newLine, WqGradeRule.pick(both, "CL", FINISHED, handover));
        // 同一读数在交接两侧按各自当时的线定档（旧松新紧），不许拿今天的线改算昨天
        BigDecimal qty = new BigDecimal("0.45");
        assertEquals(2, WqGradeRule.grade(WqGradeRule.pick(both, "CL", FINISHED, lastSecond), qty));
        assertEquals(3, WqGradeRule.grade(WqGradeRule.pick(both, "CL", FINISHED, handover), qty));
    }

    // ── 选线：顺位高者胜；并列代号排后者胜；让了位的线不参与 ──

    @Test
    public void pickByPriorityThenCodeDesc() throws Exception {
        TZwLimRule lab = rule("CL-OUT-LAB1", "CL", FINISHED, "0.25", "0.45", "0.90",
                "2026-01-01 00:00:00", null, 5, 0);
        TZwLimRule std = rule("CL-OUT-2026", "CL", FINISHED, "0.20", "0.40", "0.80",
                "2026-01-01 00:00:00", null, 10, 0);
        TZwLimRule gov = rule("CL-OUT-G2026", "CL", FINISHED, "0.15", "0.35", "0.70",
                "2026-01-01 00:00:00", null, 20, 0);
        List<TZwLimRule> rules = new ArrayList<TZwLimRule>(Arrays.asList(lab, std, gov));
        Date at = ts("2026-09-30 10:00:00");
        // 局发线顺位 20 最高，化验室自拟与新表国标都靠后
        assertSame(gov, WqGradeRule.pick(rules, "CL", FINISHED, at));

        // 顺位分不出：代号序列排后的一条胜出（CL-OUT-2026 > CL-OUT-2025B 字典序）
        TZwLimRule a = rule("CL-OUT-2025B", "CL", FINISHED, "0.2", "0.4", "0.8",
                "2026-01-01 00:00:00", null, 10, 0);
        TZwLimRule b = rule("CL-OUT-2026", "CL", FINISHED, "0.2", "0.4", "0.8",
                "2026-01-01 00:00:00", null, 10, 0);
        assertSame(b, WqGradeRule.pick(Arrays.asList(a, b), "CL", FINISHED, at));

        // 四季一致：同一份台账、同一时刻，反复选都是同一条
        for (int i = 0; i < 10; i++) {
            assertSame(gov, WqGradeRule.pick(rules, "CL", FINISHED, at));
        }
    }

    // ── 样本类别各管各的；检测项也不混；历史时刻无现行线 → null（无可用标准） ──

    @Test
    public void pickIsolatesCategoryAndItemAndTime() throws Exception {
        TZwLimRule finished = rule("CL-OUT-2026", "CL", FINISHED, "0.20", "0.40", "0.80",
                "2026-01-01 00:00:00", null, 10, 0);
        TZwLimRule retired = rule("CL-OUT-2025", "CL", FINISHED, "0.30", "0.50", "1.00",
                "2025-01-01 00:00:00", "2026-01-01 00:00:00", 10, 0);
        TZwLimRule tap = rule("CL-TAP-2026", "CL", TAP, "0.20", "0.40", "0.80",
                "2026-01-01 00:00:00", null, 99, 0);
        TZwLimRule turb = rule("TURB-OUT", "TURB", FINISHED, "1", "2", "3",
                "2026-01-01 00:00:00", null, 99, 0);
        List<TZwLimRule> rules = Arrays.asList(finished, retired, tap, turb);
        Date at2026 = ts("2026-09-30 10:00:00");

        // 出厂水余氯只在出厂水 CL 线里选，管网末梢线顺位再高也不跨类
        assertSame(finished, WqGradeRule.pick(rules, "CL", FINISHED, at2026));
        assertSame(tap, WqGradeRule.pick(rules, "CL", TAP, at2026));
        assertSame(turb, WqGradeRule.pick(rules, "TURB", FINISHED, at2026));
        assertNull(WqGradeRule.pick(rules, "PH", FINISHED, at2026));

        // 去年四季度补评价：只看检测那天。2025-12 的出厂水余氯旧线现行、新线还没启用
        Date at2025 = ts("2025-12-15 10:00:00");
        assertSame(retired, WqGradeRule.pick(rules, "CL", FINISHED, at2025));

        // 更早、一版线都没有的时点 → 无可用标准，不许拿临近日期凑合
        Date at2024 = ts("2024-12-31 23:59:59");
        assertNull(WqGradeRule.pick(rules, "CL", FINISHED, at2024));

        // 旗为让位（status=1）的线即便时段压中也不参与现行评价
        TZwLimRule flagged = rule("CL-OLD", "CL", FINISHED, "0.3", "0.5", "1",
                "2025-01-01 00:00:00", null, 99, 1);
        assertNull(WqGradeRule.pick(Arrays.asList(flagged), "CL", FINISHED, at2026));
    }

    // ── 化验员按等级执行 ──

    @Test
    public void followActions() {
        assertEquals("立即上报复核", WqGradeRule.followAction(WqGradeRule.GRADE_SEVERE));
        assertEquals("安排复测", WqGradeRule.followAction(WqGradeRule.GRADE_OVER));
        assertEquals("并入下次检测", WqGradeRule.followAction(WqGradeRule.GRADE_WATCH));
        assertEquals("合格，无需处置", WqGradeRule.followAction(WqGradeRule.GRADE_OK));
    }
}
