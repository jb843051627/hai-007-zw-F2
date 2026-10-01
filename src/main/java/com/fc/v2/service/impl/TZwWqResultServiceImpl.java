package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.mapper.auto.TZwWqResultMapper;
import com.fc.v2.model.auto.TZwWqResult;
import com.fc.v2.model.custom.wq.WqEvalOutcome;
import com.fc.v2.model.custom.wq.WqEvalQuery;
import com.fc.v2.service.ITZwLimRuleService;
import com.fc.v2.service.ITZwWqResultService;

/**
 * 水质检测读数评价 Service业务层处理。
 *
 * <p>本层不算档、不选线：定级与取线一律走 {@link ITZwLimRuleService#evaluate(WqEvalQuery)}
 * 那唯一一个评价出口，线代号与生效日只誊回执、不接受页面预填；
 * 历史复核只认原结果上钉死的编号上下文，走 {@link ITZwLimRuleService#review}。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Service
public class TZwWqResultServiceImpl implements ITZwWqResultService {

    /** 结果情形 0待处置 1已处置 2退回重录 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RETURNED = 2;

    @Resource
    private TZwWqResultMapper zwWqResultMapper;

    @Resource
    private ITZwLimRuleService zwLimRuleService;

    @Override
    public TZwWqResult selectTZwWqResultById(Long id) {
        return this.zwWqResultMapper.selectById(id);
    }

    @Override
    public List<TZwWqResult> selectTZwWqResultList(Wrapper<TZwWqResult> queryWrapper) {
        return this.zwWqResultMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WqEvalOutcome evaluateAndSave(WqEvalQuery query, Date now) {
        // 裁决只出自服务层那个评价出口；本层拿到回执再落账
        WqEvalOutcome outcome = this.zwLimRuleService.evaluate(query);
        if (outcome.getStatus() == WqEvalOutcome.Status.INVALID) {
            // 读数不在合理区间：不参与定级、不落账，退回化验员重录
            return outcome;
        }
        TZwWqResult saved = save(toResult(outcome, null, now), now);
        outcome.setMessage(outcome.getMessage() + "；结果号 " + saved.getResultNo());
        return outcome;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WqEvalOutcome review(Long resultId, Date now) {
        TZwWqResult origin = this.zwWqResultMapper.selectById(resultId);
        if (origin == null || (origin.getDelFlag() != null && origin.getDelFlag().intValue() != 0)) {
            return WqEvalOutcome.invalid(null, null, null, now, "原检测结果不存在，无法复核");
        }
        // 只认编号：凭当时写入的代号+生效日调那条线，按当时界值重评，今天的现役线不得参与。
        // 原结果本就无可用标准（编号留空）的，复核照实再落一条「无可用标准」，不拿临近日期凑合。
        WqEvalOutcome outcome;
        if (origin.getRuleCode() == null) {
            outcome = WqEvalOutcome.noRule(origin.getItemCode(), origin.getSampleCategory(),
                    origin.getQty(), origin.getTestedAt());
        } else {
            outcome = this.zwLimRuleService.review(
                    origin.getRuleCode(), origin.getRuleEffStart(), origin.getQty(), origin.getTestedAt());
        }
        TZwWqResult reviewRow = toResult(outcome, origin.getId(), now);
        // 复核行保留原结果的点位/检测项/类别/检测时刻——复核的是当时那一次，不是今天的新样
        reviewRow.setItemCode(origin.getItemCode());
        reviewRow.setSampleCategory(origin.getSampleCategory());
        reviewRow.setSiteNo(origin.getSiteNo());
        reviewRow.setTestedAt(origin.getTestedAt());
        reviewRow.setQty(origin.getQty());
        save(reviewRow, now);
        outcome.setMessage(outcome.getMessage() + "；复核结果号 " + reviewRow.getResultNo()
                + "（原结果 " + origin.getResultNo() + " 各留各的编号上下文）");
        return outcome;
    }

    /** 回执誊成检测结果行：等级、线代号、生效日一律取出口回值，页面给的值不采纳 */
    private static TZwWqResult toResult(WqEvalOutcome o, Long reviewOf, Date now) {
        TZwWqResult r = new TZwWqResult();
        r.setItemCode(o.getItemCode());
        r.setSampleCategory(o.getSampleCategory());
        r.setQty(o.getQty());
        r.setTestedAt(o.getTestedAt());
        r.setReviewOf(reviewOf);
        r.setDelFlag(0);
        if (o.getStatus() == WqEvalOutcome.Status.GRADED) {
            r.setGrade(o.getGrade());
            r.setRuleCode(o.getRuleCode());
            r.setRuleEffStart(o.getRuleEffStart());
            r.setStatus(STATUS_PENDING);
            r.setRemark(o.getMessage() + "；处置：" + o.getFollowAction());
        } else if (o.getStatus() == WqEvalOutcome.Status.NO_RULE) {
            // 无可用标准：等级与线编号留空，明确留痕，不硬给结论、不拿临近日期凑合
            r.setGrade(null);
            r.setRuleCode(null);
            r.setRuleEffStart(null);
            r.setStatus(STATUS_PENDING);
            r.setRemark("无可用标准");
        } else {
            r.setStatus(STATUS_RETURNED);
            r.setRemark(o.getMessage());
        }
        if (reviewOf != null) {
            r.setReviewedAt(now);
        }
        return r;
    }

    private TZwWqResult save(TZwWqResult r, Date now) {
        r.setId(null);
        this.zwWqResultMapper.insert(r);
        // 结果号随主键钉，形如 WQ-<主键>
        r.setResultNo("WQ-" + r.getId());
        this.zwWqResultMapper.updateById(r);
        return r;
    }
}
