package org.example.posinventorysystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Purchase {

    private Integer purchaseId;
    private String referenceNo;
    private LocalDateTime purchaseDate;
    private Integer supplierId;
    private Integer userId;
    private BigDecimal total;

    public Purchase() {
    }

    public Purchase(Integer purchaseId, String referenceNo, LocalDateTime purchaseDate,
                    Integer supplierId, Integer userId, BigDecimal total) {
        this.purchaseId = purchaseId;
        this.referenceNo = referenceNo;
        this.purchaseDate = purchaseDate;
        this.supplierId = supplierId;
        this.userId = userId;
        this.total = total;
    }

    public Integer getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Integer purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "Purchase{purchaseId=" + purchaseId + ", referenceNo='" + referenceNo
                + "', supplierId=" + supplierId + ", total=" + total + "}";
    }
}
