package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum OfferStatus {
    PENDING("PENDING"),
    ACCEPTED("ACCEPTED"),
    DECLINED("DECLINED"),
    EXPIRED("EXPIRED");

    String dbValue;

    OfferStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
