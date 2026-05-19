package models.sql;

import helpers.blueprint.models.BaseModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.RoundType;

import javax.persistence.*;
import java.util.List;

/**
 * Previous Year Questions — interview experiences, OA questions.
 * Students contribute anonymously; tagged by company + round type.
 * Premium feature for students.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "pyqs")
public class PYQ extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    public Company company;

    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;                 // null = general (not college-specific)

    public String role;                     // "SDE", "Analyst", "Data Scientist"

    @Column(nullable = false)
    public RoundType roundType;

    public int year;                        // which year's question

    @Column(columnDefinition = "TEXT", nullable = false)
    public String content;                  // the actual question / experience description

    public String difficulty;               // EASY, MEDIUM, HARD

    @DbJsonB
    public List<String> tags;               // ["DP", "Trees", "SQL", "Aptitude"]

    public int upvotes = 0;

    public boolean anonymous = true;

    @ManyToOne
    @JoinColumn(name = "contributed_by_student_id")
    public Student contributedByStudent;    // null if anonymous
}
