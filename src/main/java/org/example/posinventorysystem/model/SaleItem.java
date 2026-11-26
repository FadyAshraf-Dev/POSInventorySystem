package org.example.posinventorysystem.model;

import java.math.BigDecimal;

public class SaleItem {
    private BigDecimal discount = BigDecimal.ZERO;


    private Integer saleItemId;
    private Integer saleId;
    private Integer productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;

    public SaleItem() {
    }

    public SaleItem(Integer saleItemId, Integer saleId, Integer productId, Integer quantity,
                    BigDecimal unitPrice, BigDecimal lineTotal) {
        this.saleItemId = saleItemId;
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    /** Creates a sale item with selling-price and cost-price snapshots. */
    public SaleItem(Integer saleItemId, Integer saleId, Integer productId, Integer quantity,
                    BigDecimal unitPrice, BigDecimal unitCost, BigDecimal lineTotal) {
        this(saleItemId, saleId, productId, quantity, unitPrice, lineTotal);
        this.unitCost = unitCost;
    }

    public Integer getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(Integer saleItemId) {
        this.saleItemId = saleItemId;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    @Override
    public String toString() {
        return "SaleItem{saleItemId=" + saleItemId + ", saleId=" + saleId + ", productId=" + productId
                + ", quantity=" + quantity + ", unitPrice=" + unitPrice
                + ", unitCost=" + unitCost + ", lineTotal=" + lineTotal + "}";
    }
}
