package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.UserType;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Stores invite tokens sent by TPO to students (or potentially other roles).
 * When a TPO invites students, a token is created per email and sent via email.
 * The student uses the token to register — no separate verification needed.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "invite_tokens")
public class InviteToken extends BaseModel {

    @Column(nullable = false, unique = true)
    public String token;                    // UUID string

    @Column(nullable = false)
    public String email;                    // invited email address

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;                 // college this invite is for

    @Column(nullable = false)
    public UserType userType;               // STUDENT, COMPANY_HR, etc.

    @Column(nullable = false)
    public Timestamp expiresAt;             // token expiry (e.g., 7 days from creation)

    public boolean used = false;            // marked true once registration completes

    @ManyToOne
    @JoinColumn(name = "invited_by_user_id")
    public User invitedBy;                  // the TPO/admin who sent the invite
}
