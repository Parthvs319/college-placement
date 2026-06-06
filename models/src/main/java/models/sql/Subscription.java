package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.SubscriptionTier;

import javax.persistence.*;
import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "subscriptions")
public class Subscription extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "student_id")
    public Student student;

    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    @Column(nullable = false)
    public SubscriptionTier tier = SubscriptionTier.FREE;

    @Column(nullable = false)
    public Timestamp startDate;

    public Timestamp endDate;

    public int totalCredits = 50;

    public int usedCredits = 0;

    public Timestamp creditsResetAt;

    public String paymentReference;

    public boolean active = true;

    public boolean hasCredits() {
        return usedCredits < totalCredits;
    }

    public boolean useCredit() {
        if (usedCredits >= totalCredits) return false;
        usedCredits++;
        this.update();
        return true;
    }
}
