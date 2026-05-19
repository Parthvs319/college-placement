package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * College-level placement policies.
 * e.g. "If accepted offer >= 6 LPA, student cannot sit for further drives"
 * e.g. "Max 3 offers can be held simultaneously"
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "placement_policies")
public class PlacementPolicy extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    public int academicYear;

    /** CTC threshold — if student accepts offer >= this, they're out of the pool */
    public BigDecimal dreamCtcThreshold;

    /** Max offers a student can hold at once before being forced to decide */
    public int maxSimultaneousOffers = 1;

    /** If true, once a student accepts ANY offer, they cannot sit for more drives */
    public boolean blockAfterFirstAccept = false;

    /** If true, students below minCgpa of a drive are auto-excluded */
    public boolean autoFilterEnabled = true;

    /** Days after offer to auto-expire if no response */
    public int offerExpiryDays = 7;

    public String description;              // human-readable policy text
}
