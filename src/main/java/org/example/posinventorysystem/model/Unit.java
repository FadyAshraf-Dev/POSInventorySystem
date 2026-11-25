package org.example.posinventorysystem.model;

public class Unit {

    private Integer unitId;
    private String unitName;

    public Unit() {
    }

    public Unit(Integer unitId, String unitName) {
        this.unitId = unitId;
        this.unitName = unitName;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    @Override
    public String toString() {
        return "Unit{unitId=" + unitId + ", unitName='" + unitName + "'}";
    }
}
