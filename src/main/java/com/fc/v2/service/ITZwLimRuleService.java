package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TZwLimRule;
import com.fc.v2.model.custom.wq.WqEvalOutcome;
import com.fc.v2.model.custom.wq.WqEvalQuery;

/**
 * 水质指标限值分档判定线 Service接口（rule-eval 形状：规则求值，无增删改）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITZwLimRuleService {

    /** 按主键查询规则 */
    TZwLimRule selectTZwLimRuleById(Long id);

    /**
     * 单规则求值：按 ruleCode 在 at 时刻生效的版本判档（1..4）。
     * 无生效版本 / 输入越界 / 参数缺失一律返回 0。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /** 多规则求值：at 时刻可用规则中优先级最高者的档位；无可用规则返回 0 */
    int evaluateTop(BigDecimal input, Date at);

    /** at 时刻可用规则（优先级降序、ruleCode 降序） */
    List<TZwLimRule> listAvailable(Date at);

    /** 单条规则在 at 时刻是否可用（批量导入/报表等旁路复用，口径须与定位/列表一致） */
    boolean usable(Long id, Date at);

    /** at 时刻可用规则条数（列表页角标 / 看板） */
    int countAvailable(Date at);

    // ── 水质评价收进系统后新取法：加在老方法底下的同名一层。老方法维持原样，上面三个口不动。 ──

    /**
     * 全站<b>唯一的读数评价出口</b>：取哪条线（同检测项+同样本类别、检测时刻现行）、
     * 并线听谁的（顺位高者、并列代号排后者）、界值空缺怎么处置、落在哪一档，
     * 全部由此当场定夺，回执带线代号与生效日；页面不预填结论。
     * 无可用标准回 NO_RULE（明写「无可用标准」），读数非法回 INVALID（退回重录）。
     */
    WqEvalOutcome evaluate(WqEvalQuery query);

    /**
     * 历史复核取法：只认当时写入的编号上下文（ruleCode + 生效日），凭编号调出那条线，
     * 按该线界值对当时读数重评；已让位的线照样可调。今天的线再紧也不改算昨天的记录。
     * 编号已不可考时回 NO_RULE，不得拿临近日期的线凑合。
     */
    WqEvalOutcome review(String ruleCode, Date ruleEffStart, BigDecimal qty, Date testedAt);
}
