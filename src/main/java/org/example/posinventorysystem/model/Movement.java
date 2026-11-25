package org.example.posinventorysystem.model;

import java.time.LocalDateTime;

public class Movement {

    private Integer movementId;
    private Integer productId;
    private String movementType; // e.g. "sale", "purchase", "adjustment", "opening_stock"
    private Integer quantityChange; // positive = stock in, negative = stock out
    private Integer referenceId; // nullable — points to sale_id or purchase_id depending on movementType
    private Integer userId;
    private LocalDateTime movementDate;

    public Movement() {
    }

    public Movement(Integer movementId, Integer productId, String movementType,
                    Integer quantityChange, Integer referenceId, Integer userId,
                    LocalDateTime movementDate) {
        this.movementId = movementId;
        this.productId = productId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.referenceId = referenceId;
        this.userId = userId;
        this.movementDate = movementDate;
    }

    public Integer getMovementId() {
        return movementId;
    }

    public void setMovementId(Integer movementId) {
        this.movementId = movementId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(LocalDateTime movementDate) {
        this.movementDate = movementDate;
    }

    @Override
    public String toString() {
        return "Movement{movementId=" + movementId + ", productId=" + productId
                + ", movementType='" + movementType + "', quantityChange=" + quantityChange + "}";
    }
}
