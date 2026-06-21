package models.sql;

import helpers.blueprint.models.AttrsModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "companies")
public class Company extends AttrsModel {

    @Column(nullable = false)
    public String name;

    public String industry;                 // IT, Finance, Manufacturing, etc.

    public String website;

    public String logoUrl;

    public String description;

    public String headquarters;

    public String contactEmail;

    public String contactPhone;

    public boolean startup = false;

    public boolean active = true;

    @OneToMany(mappedBy = "company")
    public List<CompanyCollege> companyColleges;

    @OneToMany(mappedBy = "company")
    public List<CompanyDocument> documents;
}
