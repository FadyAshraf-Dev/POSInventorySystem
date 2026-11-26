package org.example.posinventorysystem.model;

import java.math.BigDecimal;

public class Product {
    private boolean active = true;


    private Integer productId;
    private String barcode;
    private String productName;
    private Integer categoryId;
    private Integer unitId;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private Integer quantityInStock;
    private Integer reorderLevel;

    public Product() {
    }

    public Product(Integer productId, String barcode, String productName, Integer categoryId,
                   Integer unitId, BigDecimal costPrice, BigDecimal sellingPrice,
                   Integer quantityInStock, Integer reorderLevel) {
        this.productId = productId;
        this.barcode = barcode;
        this.productName = productName;
        this.categoryId = categoryId;
        this.unitId = unitId;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.quantityInStock = quantityInStock;
        this.reorderLevel = reorderLevel;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(Integer quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "Product{productId=" + productId + ", productName='" + productName
                + "', categoryId=" + categoryId + ", unitId=" + unitId
                + ", sellingPrice=" + sellingPrice + ", quantityInStock=" + quantityInStock + "}";
    }
}
