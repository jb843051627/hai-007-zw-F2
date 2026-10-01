package com.fc.v2.controller.admin;

import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TZwWqResult;
import com.fc.v2.model.custom.wq.WqEvalOutcome;
import com.fc.v2.model.custom.wq.WqEvalQuery;
import com.fc.v2.service.ITZwWqResultService;
import com.fc.v2.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 水质检测读数评价 Controller。
 *
 * <p>页面只录读数与检测信息，<b>不预填结论、不先亮答案</b>：落在哪一档、凭哪条线，
 * 全由服务层唯一评价出口回值决定，线代号与生效日照回执落账。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "水质检测读数评价")
@Controller
@RequestMapping("/zwWqResult")
public class ZwWqResultController extends BaseController {

    private final String prefix = "admin/zwWqResult";

    @Autowired
    private ITZwWqResultService zwWqResultService;

    @ApiOperation(value = "检测结果台账跳转", notes = "检测结果台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("zw:zwWqResult:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "水质检测结果台账查询", action = "list")
    @ApiOperation(value = "台账查询", notes = "原始评价与历史复核结论各留各的编号上下文，都翻得到")
    @GetMapping("/list")
    @RequiresPermissions("zw:zwWqResult:list")
    @ResponseBody
    public ResultTable list(TZwWqResult record) {
        QueryWrapper<TZwWqResult> queryWrapper = new QueryWrapper<TZwWqResult>();
        queryWrapper.like(StringUtils.isNotEmpty(record.getResultNo()), "result_no", record.getResultNo());
        queryWrapper.eq(StringUtils.isNotEmpty(record.getItemCode()), "item_code", record.getItemCode());
        queryWrapper.eq(StringUtils.isNotEmpty(record.getSampleCategory()), "sample_category", record.getSampleCategory());
        queryWrapper.eq(StringUtils.isNotNull(record.getGrade()), "grade", record.getGrade());
        queryWrapper.orderByDesc("tested_at");
        startPage();
        com.github.pagehelper.PageInfo<TZwWqResult> page =
                new com.github.pagehelper.PageInfo<TZwWqResult>(
                        zwWqResultService.selectTZwWqResultList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "水质读数评价", action = "evaluate")
    @ApiOperation(value = "录一次检测并评价", notes = "定级只出自服务层评价出口；无可用标准留空写明，非法读数退回不落账")
    @PostMapping("/evaluate")
    @RequiresPermissions("zw:zwWqResult:evaluate")
    @ResponseBody
    public AjaxResult evaluate(WqEvalQuery query) {
        // 不接收页面给的等级/线代号，出口回什么就落什么
        return AjaxResult.successData(200, zwWqResultService.evaluateAndSave(query, new Date()));
    }

    @Log(title = "水质检测结果历史复核", action = "review")
    @ApiOperation(value = "历史复核", notes = "只认原结果当时写入的线编号重评，另落复核行，原结果不改")
    @PostMapping("/review")
    @RequiresPermissions("zw:zwWqResult:review")
    @ResponseBody
    public AjaxResult review(Long id) {
        WqEvalOutcome outcome = zwWqResultService.review(id, new Date());
        return AjaxResult.successData(200, outcome);
    }
}
