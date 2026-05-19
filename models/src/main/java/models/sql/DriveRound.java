package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.RoundType;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

/**
 * Each drive has multiple rounds: OA → Technical → HR → Offer.
 * Ordered by roundNumber.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "drive_rounds")
public class DriveRound extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "drive_id", nullable = false)
    public Drive drive;

    public int roundNumber;                 // 1, 2, 3...

    @Column(nullable = false)
    public RoundType roundType;

    public String name;                     // "Online Assessment", "Technical Interview R1"

    public String description;              // instructions, format, etc.

    public Timestamp scheduledAt;

    public Integer durationMinutes;

    public String venue;

    public boolean completed = false;

    @OneToMany(mappedBy = "round")
    public List<RoundResult> results;
}
