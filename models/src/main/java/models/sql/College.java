package models.sql;

import helpers.blueprint.models.AttrsModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.services.EmailService;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "colleges")
public class College extends AttrsModel {

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false, unique = true)
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


    public void save() {
        super.save();
        if (this.contactEmail != null && !this.contactEmail.isBlank()) {
            String html = EmailService.buildCollegeOnboardingHtml(
                    this.name, this.code, this.city, this.state, this.website
            );
            EmailService.sendEmail(this.contactEmail, "Welcome to Applyra — " + this.name, html)
                    .subscribe(
                            sent -> System.out.println("[CreateCollege] Welcome email " + (sent ? "sent" : "failed") + " to " + this.contactEmail),
                            err -> System.err.println("[CreateCollege] Email error: " + err.getMessage())
                    );
        }
    }
}
