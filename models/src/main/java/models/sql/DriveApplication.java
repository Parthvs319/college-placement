package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.ApplicationStatus;

import javax.persistence.*;

/**
 * A student's application to a specific drive.
 * Created when student applies or is auto-enrolled if eligible.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "drive_applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "drive_id"}))
public class DriveApplication extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @ManyToOne
    @JoinColumn(name = "drive_id", nullable = false)
    public Drive drive;

    @Column(nullable = false)
    public ApplicationStatus status = ApplicationStatus.ELIGIBLE;

    public String resumeSnapshot;           // URL of resume used at time of application

    public String notes;                    // TPO or company notes on this application
}
