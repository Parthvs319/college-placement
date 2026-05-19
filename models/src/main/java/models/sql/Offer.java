package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.OfferStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Final offer extended to a student from a drive.
 * Student can accept/decline within the deadline.
 * Placement policy enforcement happens here.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "offers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "drive_id"}))
public class Offer extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @ManyToOne
    @JoinColumn(name = "drive_id", nullable = false)
    public Drive drive;

    @Column(nullable = false)
    public BigDecimal ctcOffered;

    public String designation;              // role offered

    public String location;

    @Column(nullable = false)
    public OfferStatus status = OfferStatus.PENDING;

    public Timestamp responseDeadline;      // accept/decline by this date

    public Timestamp respondedAt;           // when student accepted/declined

    public String offerLetterUrl;           // uploaded PDF link

    public String notes;
}
