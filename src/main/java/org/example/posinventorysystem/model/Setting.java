package org.example.posinventorysystem.model;

import java.math.BigDecimal;

public class Setting {
    private String storeAddress;
    private String storePhone;
    private String storeEmail;


    private Integer settingId;
    private String storeName;
    private String currency;
    private BigDecimal taxRate;

    public Setting() {
    }

    public Setting(Integer settingId, String storeName, String currency, BigDecimal taxRate) {
        this.settingId = settingId;
        this.storeName = storeName;
        this.currency = currency;
        this.taxRate = taxRate;
    }

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getStoreAddress() { return storeAddress; }
    public String getStorePhone() { return storePhone; }
    public String getStoreEmail() { return storeEmail; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }
    public void setStorePhone(String storePhone) { this.storePhone = storePhone; }
    public void setStoreEmail(String storeEmail) { this.storeEmail = storeEmail; }

    @Override
    public String toString() {
        return "Setting{settingId=" + settingId + ", storeName='" + storeName
                + "', currency='" + currency + "', taxRate=" + taxRate + "}";
    }
}
