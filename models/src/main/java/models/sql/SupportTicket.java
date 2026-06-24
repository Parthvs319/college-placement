package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * A support ticket raised by a TPO/college admin or super admin.
 * Priority: P1 = critical/blocking, P2 = normal.
 * Status: OPEN → IN_PROGRESS → RESOLVED
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "support_tickets")
public class SupportTicket extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    public User raisedBy;

    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    @Column(nullable = false)
    public String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    /** P1 = critical, P2 = normal */
    @Column(nullable = false)
    public String priority = "P2";

    /** OPEN | IN_PROGRESS | RESOLVED */
    @Column(nullable = false)
    public String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    public String notes;

    public Timestamp resolvedAt;
}
