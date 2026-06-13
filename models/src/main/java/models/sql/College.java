package models.sql;

import helpers.blueprint.models.AttrsModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "colleges")
public class College extends AttrsModel {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String code;                     // e.g. "SGSITS", "IET-DAVV"

    public String university;               // affiliated university

    public String address;

    public String city;

    public String state;

    public String pincode;

    public String website;

    public String logoUrl;

    @Column(nullable = false)
    public String contactEmail;

    public String contactPhone;

    @DbJsonB
    public List<String> departments;        // ["CSE", "IT", "ECE", "ME", ...]

    public boolean verified = false;

    public boolean active = true;

    @OneToMany(mappedBy = "college")
    public List<Student> students;

    @OneToMany(mappedBy = "college")
    public List<CompanyCollege> companyColleges;
}
