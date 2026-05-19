package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum ApplicationStatus {
    ELIGIBLE("ELIGIBLE"),
    APPLIED("APPLIED"),
    SHORTLISTED("SHORTLISTED"),
    IN_PROCESS("IN_PROCESS"),
    SELECTED("SELECTED"),
    REJECTED("REJECTED"),
    WITHDRAWN("WITHDRAWN"),
    ON_HOLD("ON_HOLD");

    String dbValue;

    ApplicationStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
