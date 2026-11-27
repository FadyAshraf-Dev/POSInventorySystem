package org.example.posinventorysystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private Integer paymentId;
    private Integer saleId;
    private String method;
    private BigDecimal amount;
    private LocalDateTime paymentDate;

    public Payment() {
    }

    public Payment(Integer paymentId, Integer saleId, String method, BigDecimal amount,
                   LocalDateTime paymentDate) {
        this.paymentId = paymentId;
        this.saleId = saleId;
        this.method = method;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    @Override
    public String toString() {
        return "Payment{paymentId=" + paymentId + ", saleId=" + saleId + ", method='" + method
                + "', amount=" + amount + "}";
    }
}
