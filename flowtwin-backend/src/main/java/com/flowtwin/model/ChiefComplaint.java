package com.flowtwin.model;

import jakarta.persistence.*;

/**
 * Free-text chief complaint from chief_complaints.csv, keyed 1:1 to {@link ClinicalPatientData}
 * by {@code patientId}. Kept as its own table because the raw text is a distinct concern from the
 * structured feature row.
 *
 * <p>{@code patientId} unique = reload protection. Surrogate {@link #id} is internal DB identity only.
 */
@Entity
@Table(name = "chief_complaints",
        uniqueConstraints = @UniqueConstraint(name = "uq_chief_complaint_patient_id", columnNames = "patientId"))
public class ChiefComplaint {

    /** Internal DB identity only. Reload protection comes from the patientId unique constraint. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 16)
    private String patientId;

    @Column(columnDefinition = "text")
    private String chiefComplaintRaw;

    private String chiefComplaintSystem;

    protected ChiefComplaint() { }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getChiefComplaintRaw() { return chiefComplaintRaw; }
    public String getChiefComplaintSystem() { return chiefComplaintSystem; }
}
