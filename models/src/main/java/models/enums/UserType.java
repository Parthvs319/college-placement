package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum UserType {
    STUDENT("STUDENT"),
    COLLEGE_ADMIN("COLLEGE_ADMIN"),
    TPO("TPO"),
    COMPANY_HR("COMPANY_HR"),
    SUPER_ADMIN("SUPER_ADMIN");

    String dbValue;

    UserType(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
