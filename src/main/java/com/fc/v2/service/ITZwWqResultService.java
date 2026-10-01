package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TZwWqResult;
import com.fc.v2.model.custom.wq.WqEvalOutcome;
import com.fc.v2.model.custom.wq.WqEvalQuery;

/**
 * 水质检测读数评价 Service接口。
 *
 * <p>定级裁决只出自限值线服务那一个评价出口，本服务只负责把回执誊写成检测结果留账，
 * 自己不算档、不选线。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface ITZwWqResultService {

    /** 按主键回查单条结果 */
    TZwWqResult selectTZwWqResultById(Long id);

    /** 台账查询（原始评价与复核结论都翻得到） */
    List<TZwWqResult> selectTZwWqResultList(Wrapper<TZwWqResult> queryWrapper);

    /**
     * 录一次检测：交限值线服务唯一评价出口当场定级，再把原始读数、等级、所依线代号与生效日
     * 原样誊进检测结果，写过不再改。
     * <ul>
     *   <li>GRADED：落等级与线编号上下文；</li>
     *   <li>NO_RULE：等级与线编号留空，备注明写「无可用标准」，照样落账（不得硬给结论）；</li>
     *   <li>INVALID：读数非法，不参与定级、不落账，退回化验员重录（回执 resultId 为空）。</li>
     * </ul>
     */
    WqEvalOutcome evaluateAndSave(WqEvalQuery query, Date now);

    /**
     * 历史复核：只认原结果当时写入的线代号+生效日，凭编号调那条线、按当时界值重评，
     * 另落一条 review_of 指向原结果的复核行；原结果一个字不改。
     * 原结果本就无可用标准、或编号已不可考时，复核行同样留空并写明「无可用标准」。
     */
    WqEvalOutcome review(Long resultId, Date now);
}
