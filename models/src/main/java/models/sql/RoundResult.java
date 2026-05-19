package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.Status;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Result of a student in a specific round.
 * Tracks score, status (APPROVED = cleared, REJECTED = eliminated).
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "round_results",
       uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "student_id"}))
public class RoundResult extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "round_id", nullable = false)
    public DriveRound round;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @Column(nullable = false)
    public Status status = Status.PENDING;  // PENDING, APPROVED (cleared), REJECTED

    public BigDecimal score;                // OA score, interview rating, etc.

    public String feedback;                 // interviewer feedback

    public String interviewerName;
}
