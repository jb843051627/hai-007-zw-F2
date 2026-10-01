package com.fc.v2.model.custom.wq;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fc.v2.service.zw.WqGradeRule;

/**
 * 水质读数评价回执——服务层那<b>唯一一个评价出口</b>的回值。
 *
 * <p>取哪条线、并线听谁的、界值空缺怎么处置、落在哪一档，都以本回执为准；
 * 页面不预填结论、不先亮答案，写进检测结果的线代号与生效日照此誊写。
 *
 * <p>三种结局分得清，不许混成一句"评价失败"：
 * <ul>
 *   <li>{@link Status#GRADED}：命中现行线并定档，grade 为 1..4，线代号/生效日齐备；</li>
 *   <li>{@link Status#NO_RULE}：检测时刻没有可用标准，grade 恒为空，message 明写「无可用标准」，
 *       不得拿临近日期的线凑合、不得硬给结论；</li>
 *   <li>{@link Status#INVALID}：读数不在合理区间（空值/负数/超大怪值）或入参缺失，
 *       不参与定级，退回化验员重录。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-30
 */
public class WqEvalOutcome implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        /** 命中现行判定线并已定档 */
        GRADED,
        /** 检测时刻无可用标准（不得硬给结论） */
        NO_RULE,
        /** 读数非法/入参缺失，退回重录 */
        INVALID
    }

    /** 结局 */
    private Status status;

    /** 评价等级 1合格 2关注 3超标 4严重超标；无可用标准/非法恒为空（空不是档位） */
    private Integer grade;

    /** 检测项代号（回显） */
    private String itemCode;

    /** 样本类别（回显） */
    private String sampleCategory;

    /** 原始读数（回显） */
    private BigDecimal qty;

    /** 检测发生时刻（回显） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date testedAt;

    /** 所依判定线代号（无可用标准为空） */
    private String ruleCode;

    /** 所依判定线名 */
    private String ruleName;

    /** 所依判定线生效日（随代号一并钉进检测结果） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ruleEffStart;

    /** 所依判定线交棒日(不含)，空表示至今现行 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ruleEffEnd;

    /** 命中线顺位 */
    private Integer priority;

    /** 化验员按等级怎么办 */
    private String followAction;

    /** 提示语：无可用标准/退回原因都直说 */
    private String message;

    public static WqEvalOutcome invalid(String itemCode, String sampleCategory, BigDecimal qty, Date testedAt, String message) {
        WqEvalOutcome o = base(itemCode, sampleCategory, qty, testedAt);
        o.status = Status.INVALID;
        o.message = message;
        return o;
    }

    public static WqEvalOutcome noRule(String itemCode, String sampleCategory, BigDecimal qty, Date testedAt) {
        WqEvalOutcome o = base(itemCode, sampleCategory, qty, testedAt);
        o.status = Status.NO_RULE;
        o.message = "无可用标准";
        return o;
    }

    /**
     * @param rule  出口当场选定的那条线（不得由页面指定）
     * @param grade 出口当场套界值定的档
     */
    public static WqEvalOutcome graded(String itemCode, String sampleCategory, BigDecimal qty, Date testedAt,
                                       com.fc.v2.model.auto.TZwLimRule rule, int grade) {
        WqEvalOutcome o = base(itemCode, sampleCategory, qty, testedAt);
        o.status = Status.GRADED;
        o.grade = Integer.valueOf(grade);
        o.ruleCode = rule.getRuleCode();
        o.ruleName = rule.getRuleName();
        o.ruleEffStart = rule.getEffStart();
        o.ruleEffEnd = rule.getEffEnd();
        o.priority = rule.getPriority();
        o.followAction = WqGradeRule.followAction(grade);
        o.message = "依「" + rule.getRuleCode() + "」判为 " + gradeName(grade);
        return o;
    }

    private static WqEvalOutcome base(String itemCode, String sampleCategory, BigDecimal qty, Date testedAt) {
        WqEvalOutcome o = new WqEvalOutcome();
        o.itemCode = itemCode;
        o.sampleCategory = sampleCategory;
        o.qty = qty;
        o.testedAt = testedAt;
        return o;
    }

    /** 等级名（屏上翻结果时随档亮） */
    public static String gradeName(int grade) {
        switch (grade) {
            case WqGradeRule.GRADE_OK:
                return "合格";
            case WqGradeRule.GRADE_WATCH:
                return "关注";
            case WqGradeRule.GRADE_OVER:
                return "超标";
            case WqGradeRule.GRADE_SEVERE:
                return "严重超标";
            default:
                return "无等级";
        }
    }

    public boolean isGraded() {
        return status == Status.GRADED;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getSampleCategory() {
        return sampleCategory;
    }

    public void setSampleCategory(String sampleCategory) {
        this.sampleCategory = sampleCategory;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Date getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(Date testedAt) {
        this.testedAt = testedAt;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Date getRuleEffStart() {
        return ruleEffStart;
    }

    public void setRuleEffStart(Date ruleEffStart) {
        this.ruleEffStart = ruleEffStart;
    }

    public Date getRuleEffEnd() {
        return ruleEffEnd;
    }

    public void setRuleEffEnd(Date ruleEffEnd) {
        this.ruleEffEnd = ruleEffEnd;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getFollowAction() {
        return followAction;
    }

    public void setFollowAction(String followAction) {
        this.followAction = followAction;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
