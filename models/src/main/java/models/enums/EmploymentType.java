package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum EmploymentType {
    FULL_TIME("FULL_TIME"),
    INTERNSHIP("INTERNSHIP"),
    INTERN_PLUS_FTE("INTERN_PLUS_FTE"),
    CONTRACT("CONTRACT"),
    PART_TIME("PART_TIME");

    String dbValue;

    EmploymentType(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
