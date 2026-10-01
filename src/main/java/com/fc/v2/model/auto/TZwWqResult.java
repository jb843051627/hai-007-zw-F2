package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 水质检测读数评价结果对象 t_zw_wq_result
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_zw_wq_result")
@ApiModel(value = "TZwWqResult", description = "水质检测读数评价结果")
public class TZwWqResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 检测结果号 */
    @TableField("result_no")
    @ApiModelProperty(value = "检测结果号")
    private String resultNo;

    /** 检测项代号（如出厂水余氯 CL） */
    @TableField("item_code")
    @ApiModelProperty(value = "检测项代号")
    private String itemCode;

    /** 样本类别(水源水/出厂水/管网末梢) */
    @TableField("sample_category")
    @ApiModelProperty(value = "样本类别(水源水/出厂水/管网末梢)")
    private String sampleCategory;

    /** 采样点位代号 */
    @TableField("site_no")
    @ApiModelProperty(value = "采样点位代号")
    private String siteNo;

    /** 原始读数（誊自检测，写过不再改） */
    @TableField("qty")
    @ApiModelProperty(value = "原始读数")
    private BigDecimal qty;

    /** 检测发生时刻（用哪条线只看此刻） */
    @TableField("tested_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "检测发生时刻")
    private Date testedAt;

    /** 评价等级 1合格 2关注 3超标 4严重超标；无可用标准恒为空 */
    @TableField("grade")
    @ApiModelProperty(value = "评价等级 1合格 2关注 3超标 4严重超标；无可用标准为空")
    private Integer grade;

    /** 所依判定线代号（服务层评价出口回值誊写） */
    @TableField("rule_code")
    @ApiModelProperty(value = "所依判定线代号")
    private String ruleCode;

    /** 所依判定线生效日（随代号一并钉死） */
    @TableField("rule_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "所依判定线生效日")
    private Date ruleEffStart;

    /** 本行为历史复核时指向的原结果主键；空=原始评价 */
    @TableField("review_of")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "历史复核指向的原结果主键，空=原始评价")
    private Long reviewOf;

    /** 复核时刻 */
    @TableField("reviewed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "复核时刻")
    private Date reviewedAt;

    /** 结果情形 0待处置 1已处置 2退回重录 */
    @TableField("status")
    @ApiModelProperty(value = "结果情形 0待处置 1已处置 2退回重录")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注（无可用标准/退回原因写于此） */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResultNo() {
        return resultNo;
    }

    public void setResultNo(String resultNo) {
        this.resultNo = resultNo;
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

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
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

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Date getRuleEffStart() {
        return ruleEffStart;
    }

    public void setRuleEffStart(Date ruleEffStart) {
        this.ruleEffStart = ruleEffStart;
    }

    public Long getReviewOf() {
        return reviewOf;
    }

    public void setReviewOf(Long reviewOf) {
        this.reviewOf = reviewOf;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
