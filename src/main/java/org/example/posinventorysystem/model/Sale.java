package org.example.posinventorysystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {

    private Integer saleId;
    private String invoiceNo;
    private LocalDateTime saleDate;
    private Integer userId;
    private Integer customerId; // nullable — walk-in sales may have no customer
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private String status;

    public Sale() {
    }

    public Sale(Integer saleId, String invoiceNo, LocalDateTime saleDate, Integer userId,
                Integer customerId, BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                BigDecimal total, String status) {
        this.saleId = saleId;
        this.invoiceNo = invoiceNo;
        this.saleDate = saleDate;
        this.userId = userId;
        this.customerId = customerId;
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.status = status;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Sale{saleId=" + saleId + ", invoiceNo='" + invoiceNo + "', total=" + total
                + ", status='" + status + "'}";
    }
}
