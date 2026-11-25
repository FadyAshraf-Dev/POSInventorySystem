package org.example.posinventorysystem.model;

import java.math.BigDecimal;

public class PurchaseItem {

    private Integer purchaseItemId;
    private Integer purchaseId;
    private Integer productId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;

    public PurchaseItem() {
    }

    public PurchaseItem(Integer purchaseItemId, Integer purchaseId, Integer productId,
                        BigDecimal quantity, BigDecimal unitCost, BigDecimal lineTotal) {
        this.purchaseItemId = purchaseItemId;
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineTotal = lineTotal;
    }

    public Integer getPurchaseItemId() {
        return purchaseItemId;
    }

    public void setPurchaseItemId(Integer purchaseItemId) {
        this.purchaseItemId = purchaseItemId;
    }

    public Integer getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Integer purchaseId) {
        this.purchaseId = purchaseId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    @Override
    public String toString() {
        return "PurchaseItem{purchaseItemId=" + purchaseItemId + ", purchaseId=" + purchaseId
                + ", productId=" + productId + ", lineTotal=" + lineTotal + "}";
    }
}
