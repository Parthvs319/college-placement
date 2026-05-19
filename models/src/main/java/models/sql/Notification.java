package models.sql;

import helpers.blueprint.models.BaseModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Map;

/**
 * Notification log — tracks every email/WhatsApp sent.
 * Used for audit trail and retry logic.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "notifications")
public class Notification extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    @ManyToOne
    @JoinColumn(name = "drive_id")
    public Drive drive;                     // null for general college notifications

    @Column(nullable = false)
    public String channel;                  // EMAIL, WHATSAPP

    @Column(nullable = false)
    public String type;                     // DRIVE_ANNOUNCEMENT, DEADLINE_REMINDER, RESULT, CUSTOM

    public String subject;

    @Column(columnDefinition = "TEXT")
    public String body;

    public int recipientCount = 0;

    public int deliveredCount = 0;

    public int failedCount = 0;

    public Timestamp sentAt;

    @DbJsonB
    public Map<String, String> metadata;    // filter criteria used, template name, etc.
}
