package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "aishe_colleges")
public class AisheCollege extends BaseModel {

    @Column(nullable = false, unique = true, length = 20)
    public String aisheCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String name;

    @Column(length = 100)
    public String state;

    @Column(length = 100)
    public String district;

    @Column(length = 255)
    public String website;

    public Integer yearOfEstablishment;

    @Column(length = 20)
    public String location;

    @Column(length = 100)
    public String collegeType;

    @Column(length = 100)
    public String management;

    @Column(length = 20)
    public String universityAisheCode;

    @Column(columnDefinition = "TEXT")
    public String universityName;

    @Column(length = 100)
    public String universityType;
}
