package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum DriveStatus {
    UPCOMING("UPCOMING"),
    REGISTRATION_OPEN("REGISTRATION_OPEN"),
    REGISTRATION_CLOSED("REGISTRATION_CLOSED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    String dbValue;

    DriveStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
