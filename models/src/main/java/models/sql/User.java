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

    /** College this user belongs to (null for SUPER_ADMIN) */
    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    public boolean verified = false;

    public boolean active = true;

    private String currentOtp;

    public String avatarUrl;
}
