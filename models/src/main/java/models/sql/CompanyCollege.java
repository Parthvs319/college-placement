package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

/**
 * Junction table: a company's relationship with a specific college.
 * College admin creates this, then gives the company HR access.
 * Stores year-on-year history automatically through drives.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "company_colleges",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "college_id"}))
public class CompanyCollege extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    public Company company;

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    @ManyToOne
    @JoinColumn(name = "managed_by_user_id")
    public User managedByUser;              // company HR user who manages this

    public boolean companyCanManage = false; // if true, company HR has dashboard access

    public boolean active = true;

    @OneToMany(mappedBy = "companyCollege")
    public List<Drive> drives;
}
