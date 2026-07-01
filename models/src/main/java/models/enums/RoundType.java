package models.enums;

import io.ebean.annotation.DbEnumValue;

public enum RoundType {
    ONLINE_ASSESSMENT("ONLINE_ASSESSMENT"),
    APTITUDE("APTITUDE"),
    GROUP_DISCUSSION("GROUP_DISCUSSION"),
    TECHNICAL_ROUND("TECHNICAL_ROUND"),
    CODING_ROUND("CODING_ROUND"),
    HR_ROUND("HR_ROUND"),
    MANAGERIAL_ROUND("MANAGERIAL_ROUND"),
    CASE_STUDY("CASE_STUDY"),
    PRESENTATION("PRESENTATION"),
    RESUME_SHORTLISTING("RESUME_SHORTLISTING"),
    OTHER("OTHER");

    String dbValue;

    RoundType(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue
    public String getValue() {
        return dbValue;
    }
}
