package com.flowtwin.model;

import jakarta.persistence.*;

/**
 * ML feature/label record sourced from the TriageGeist clinical dataset (train.csv / test.csv).
 *
 * <p>Deliberately separate from the operational {@link PatientEvent} table: different lifecycle
 * (bulk-loaded static reference vs. append-only live event stream), different key space
 * ({@code TG-*} clinical ids vs. {@code P-*} operational ids), and no relationship between them.
 *
 * <p>{@code patientId} is the source/join key and carries the {@code unique} constraint that makes
 * a reload idempotent (re-running the loader with {@code ON CONFLICT (patient_id) DO NOTHING}
 * skips rows already present). The surrogate {@link #id} exists only for internal DB identity and
 * provides no reload protection on its own.
 */
@Entity
@Table(name = "clinical_patient_data",
        uniqueConstraints = @UniqueConstraint(name = "uq_clinical_patient_id", columnNames = "patientId"),
        indexes = {
                @Index(name = "idx_clinical_triage_acuity", columnList = "triageAcuity"),
                @Index(name = "idx_clinical_dataset_split", columnList = "datasetSplit")
        })
public class ClinicalPatientData {

    /** Internal DB identity only. Does NOT provide reload protection — that is the patientId unique constraint. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 16)
    private String patientId;

    private String siteId;
    private String triageNurseId;
    private String arrivalMode;
    private Integer arrivalHour;
    private String arrivalDay;
    private Integer arrivalMonth;
    private String arrivalSeason;
    private String shift;
    private Integer age;
    private String ageGroup;
    private String sex;
    private String language;
    private String insuranceType;
    private String transportOrigin;
    private String painLocation;
    private String mentalStatusTriage;
    private String chiefComplaintSystem;
    @Column(name = "num_prior_ed_visits_12m")
    private Integer numPriorEdVisits12m;
    @Column(name = "num_prior_admissions_12m")
    private Integer numPriorAdmissions12m;
    private Integer numActiveMedications;
    private Integer numComorbidities;
    private Double systolicBp;
    private Double diastolicBp;
    private Double meanArterialPressure;
    private Double pulsePressure;
    private Double heartRate;
    private Double respiratoryRate;
    @Column(name = "temperature_c")
    private Double temperatureC;
    private Double spo2;
    private Integer gcsTotal;
    private Integer painScore;
    private Double weightKg;
    private Double heightCm;
    private Double bmi;
    private Double shockIndex;
    private Integer news2Score;

    /** Label — null for the test split. */
    private String disposition;
    /** Label — null for the test split. */
    private Double edLosHours;
    /** ML target — null for the test split. */
    private Integer triageAcuity;

    /** 'train' or 'test'. Review 1 loads 'train' only. */
    @Column(nullable = false, length = 8)
    private String datasetSplit;

    protected ClinicalPatientData() { }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getSiteId() { return siteId; }
    public String getTriageNurseId() { return triageNurseId; }
    public String getArrivalMode() { return arrivalMode; }
    public Integer getArrivalHour() { return arrivalHour; }
    public String getArrivalDay() { return arrivalDay; }
    public Integer getArrivalMonth() { return arrivalMonth; }
    public String getArrivalSeason() { return arrivalSeason; }
    public String getShift() { return shift; }
    public Integer getAge() { return age; }
    public String getAgeGroup() { return ageGroup; }
    public String getSex() { return sex; }
    public String getLanguage() { return language; }
    public String getInsuranceType() { return insuranceType; }
    public String getTransportOrigin() { return transportOrigin; }
    public String getPainLocation() { return painLocation; }
    public String getMentalStatusTriage() { return mentalStatusTriage; }
    public String getChiefComplaintSystem() { return chiefComplaintSystem; }
    public Integer getNumPriorEdVisits12m() { return numPriorEdVisits12m; }
    public Integer getNumPriorAdmissions12m() { return numPriorAdmissions12m; }
    public Integer getNumActiveMedications() { return numActiveMedications; }
    public Integer getNumComorbidities() { return numComorbidities; }
    public Double getSystolicBp() { return systolicBp; }
    public Double getDiastolicBp() { return diastolicBp; }
    public Double getMeanArterialPressure() { return meanArterialPressure; }
    public Double getPulsePressure() { return pulsePressure; }
    public Double getHeartRate() { return heartRate; }
    public Double getRespiratoryRate() { return respiratoryRate; }
    public Double getTemperatureC() { return temperatureC; }
    public Double getSpo2() { return spo2; }
    public Integer getGcsTotal() { return gcsTotal; }
    public Integer getPainScore() { return painScore; }
    public Double getWeightKg() { return weightKg; }
    public Double getHeightCm() { return heightCm; }
    public Double getBmi() { return bmi; }
    public Double getShockIndex() { return shockIndex; }
    public Integer getNews2Score() { return news2Score; }
    public String getDisposition() { return disposition; }
    public Double getEdLosHours() { return edLosHours; }
    public Integer getTriageAcuity() { return triageAcuity; }
    public String getDatasetSplit() { return datasetSplit; }
}
