package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.UserType;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "users")
public class User extends BaseModel {

    @Column(nullable = false, unique = true)
    public String email;

    public String name;

    public String mobile;

    public String password;

    @Column(nullable = false)
    public UserType userType;

    /** College this user belongs to (for TPO, COLLEGE_ADMIN, STUDENT) */
    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    /** Company this user belongs to (for COMPANY_HR) */
    @ManyToOne
    @JoinColumn(name = "company_id")
    public Company company;

    public boolean verified = false;

    public boolean active = true;

    /** True for the first/main TPO, COMPANY_HR, etc. — bypasses portal_permissions checks. */
    public boolean isPrimary = false;

    private String currentOtp;

    public String avatarUrl;
}
