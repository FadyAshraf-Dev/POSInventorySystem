package org.example.posinventorysystem.model;

public class Customer {
    private String address;


    private Integer customerId;
    private String customerName;
    private String phone;

    public Customer() {
    }

    public Customer(Integer customerId, String customerName, String phone) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "Customer{customerId=" + customerId + ", customerName='" + customerName
                + "', phone='" + phone + "'}";
    }
}
