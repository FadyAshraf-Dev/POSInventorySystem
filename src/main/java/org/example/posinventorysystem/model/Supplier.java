package org.example.posinventorysystem.model;

public class Supplier {

    private Integer supplierId;
    private String supplierName;
    private String phone;

    public Supplier() {
    }

    public Supplier(Integer supplierId, String supplierName, String phone) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.phone = phone;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "Supplier{supplierId=" + supplierId + ", supplierName='" + supplierName
                + "', phone='" + phone + "'}";
    }
}
