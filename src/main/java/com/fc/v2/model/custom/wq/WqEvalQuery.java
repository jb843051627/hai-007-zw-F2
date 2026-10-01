package com.fc.v2.model.custom.wq;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 水质读数评价入参。
 *
 * <p>用哪条线只看 {@link #testedAt}（检测发生那天），不看今天；
 * 去年采的样本今年补结论，也须把当时的检测时刻如实带来。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class WqEvalQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 检测项代号（如出厂水余氯 CL） */
    private String itemCode;

    /** 样本类别：水源水 / 出厂水 / 管网末梢，各管各的 */
    private String sampleCategory;

    /** 原始读数（空值/负数/超大怪值由出口退回，不在这里预先挡） */
    private BigDecimal qty;

    /** 检测发生时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date testedAt;

    /** 采样点位代号（可空，仅随结果留痕） */
    private String siteNo;

    public WqEvalQuery() {
    }

    public WqEvalQuery(String itemCode, String sampleCategory, BigDecimal qty, Date testedAt) {
        this.itemCode = itemCode;
        this.sampleCategory = sampleCategory;
        this.qty = qty;
        this.testedAt = testedAt;
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

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }
}
