package com.flowtwin.model;

import jakarta.persistence.*;

/**
 * Comorbidity history vector from patient_history.csv, keyed 1:1 to {@link ClinicalPatientData}
 * by {@code patientId}. The 25 hx_* flags keep their raw numeric 0/1 representation (stored as
 * {@code Integer}) so the ML layer consumes them directly without boolean conversion.
 *
 * <p>{@code patientId} unique = reload protection. Surrogate {@link #id} is internal DB identity only.
 */
@Entity
@Table(name = "patient_history",
        uniqueConstraints = @UniqueConstraint(name = "uq_patient_history_patient_id", columnNames = "patientId"))
public class PatientHistory {

    /** Internal DB identity only. Reload protection comes from the patientId unique constraint. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 16)
    private String patientId;

    private Integer hxHypertension;
    private Integer hxDiabetesType2;
    private Integer hxDiabetesType1;
    private Integer hxAsthma;
    private Integer hxCopd;
    private Integer hxHeartFailure;
    private Integer hxAtrialFibrillation;
    private Integer hxCkd;
    private Integer hxLiverDisease;
    private Integer hxMalignancy;
    private Integer hxObesity;
    private Integer hxDepression;
    private Integer hxAnxiety;
    private Integer hxDementia;
    private Integer hxEpilepsy;
    private Integer hxHypothyroidism;
    private Integer hxHyperthyroidism;
    private Integer hxHiv;
    private Integer hxCoagulopathy;
    private Integer hxImmunosuppressed;
    private Integer hxPregnant;
    private Integer hxSubstanceUseDisorder;
    private Integer hxCoronaryArteryDisease;
    private Integer hxStrokePrior;
    private Integer hxPeripheralVascularDisease;

    protected PatientHistory() { }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public Integer getHxHypertension() { return hxHypertension; }
    public Integer getHxDiabetesType2() { return hxDiabetesType2; }
    public Integer getHxDiabetesType1() { return hxDiabetesType1; }
    public Integer getHxAsthma() { return hxAsthma; }
    public Integer getHxCopd() { return hxCopd; }
    public Integer getHxHeartFailure() { return hxHeartFailure; }
    public Integer getHxAtrialFibrillation() { return hxAtrialFibrillation; }
    public Integer getHxCkd() { return hxCkd; }
    public Integer getHxLiverDisease() { return hxLiverDisease; }
    public Integer getHxMalignancy() { return hxMalignancy; }
    public Integer getHxObesity() { return hxObesity; }
    public Integer getHxDepression() { return hxDepression; }
    public Integer getHxAnxiety() { return hxAnxiety; }
    public Integer getHxDementia() { return hxDementia; }
    public Integer getHxEpilepsy() { return hxEpilepsy; }
    public Integer getHxHypothyroidism() { return hxHypothyroidism; }
    public Integer getHxHyperthyroidism() { return hxHyperthyroidism; }
    public Integer getHxHiv() { return hxHiv; }
    public Integer getHxCoagulopathy() { return hxCoagulopathy; }
    public Integer getHxImmunosuppressed() { return hxImmunosuppressed; }
    public Integer getHxPregnant() { return hxPregnant; }
    public Integer getHxSubstanceUseDisorder() { return hxSubstanceUseDisorder; }
    public Integer getHxCoronaryArteryDisease() { return hxCoronaryArteryDisease; }
    public Integer getHxStrokePrior() { return hxStrokePrior; }
    public Integer getHxPeripheralVascularDisease() { return hxPeripheralVascularDisease; }
}
