package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * Document vault — MOUs, brochures, placement reports, etc.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "documents")
public class Document extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    @ManyToOne
    @JoinColumn(name = "company_id")
    public Company company;                 // null for general college docs

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String type;                     // MOU, BROCHURE, PLACEMENT_REPORT, JD, OTHER

    @Column(nullable = false)
    public String fileUrl;

    public String fileType;                 // PDF, DOCX, etc.

    public Long fileSizeBytes;

    public int academicYear;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_user_id")
    public User uploadedByUser;
}
