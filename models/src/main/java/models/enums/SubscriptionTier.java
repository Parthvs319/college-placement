package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum SubscriptionTier {
    FREE("FREE"),
    PREMIUM("PREMIUM");

    String dbValue;

    SubscriptionTier(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
