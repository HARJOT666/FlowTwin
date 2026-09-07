package com.flowtwin.clinical;

import com.flowtwin.model.ClinicalPatientData;
import com.flowtwin.model.ChiefComplaint;
import com.flowtwin.model.PatientHistory;
import com.flowtwin.repository.ChiefComplaintRepository;
import com.flowtwin.repository.ClinicalPatientDataRepository;
import com.flowtwin.repository.PatientHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real Postgres via Testcontainers. Exercises the gated loader end to end against a tiny CSV set:
 * bulk load, nullable preservation, quoted-field parsing, and patient_id-unique reload protection.
 */
@Testcontainers
@SpringBootTest(classes = ClinicalDataLoaderTest.ClinicalJpaConfig.class)
class ClinicalDataLoaderTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.flowtwin.model")
    @EnableJpaRepositories("com.flowtwin.repository")
    static class ClinicalJpaConfig { }

    @Autowired JdbcTemplate jdbc;
    @Autowired ClinicalPatientDataRepository clinicalRepo;
    @Autowired PatientHistoryRepository historyRepo;
    @Autowired ChiefComplaintRepository complaintRepo;

    @TempDir Path dir;

    private static final String CLINICAL_HEADER =
            "patient_id,site_id,triage_nurse_id,arrival_mode,arrival_hour,arrival_day,arrival_month,arrival_season,"
            + "shift,age,age_group,sex,language,insurance_type,transport_origin,pain_location,mental_status_triage,"
            + "chief_complaint_system,num_prior_ed_visits_12m,num_prior_admissions_12m,num_active_medications,"
            + "num_comorbidities,systolic_bp,diastolic_bp,mean_arterial_pressure,pulse_pressure,heart_rate,"
            + "respiratory_rate,temperature_c,spo2,gcs_total,pain_score,weight_kg,height_cm,bmi,shock_index,"
            + "news2_score,disposition,ed_los_hours,triage_acuity";

    @BeforeEach
    void writeCsvs() throws Exception {
        Files.writeString(dir.resolve("train.csv"), CLINICAL_HEADER + "\n"
                + "TG-TEST0001,SITE-TMP-01,NURSE-0033,walk-in,6,Monday,5,spring,morning,43,middle_aged,M,Finnish,"
                + "public,public_space,extremity,drowsy,neurological,0,0,4,8,79.0,57.5,64.7,21.5,57.3,17.9,37.0,"
                + "92.1,14,7,52.3,165.4,19.1,0.725,8,discharged,7.35,2\n"
                // row 2: blank systolic_bp (nullable), pain_score -1
                + "TG-TEST0002,SITE-HEL-01,NURSE-0001,walk-in,6,Thursday,4,spring,morning,72,elderly,F,Russian,"
                + "military,home,extremity,alert,genitourinary,0,0,10,8,,93.4,106.2,38.3,97.3,17.2,36.9,99.4,15,-1,"
                + "73.3,164.4,27.1,0.739,8,discharged,0.7,5\n");

        Files.writeString(dir.resolve("patient_history.csv"),
                "patient_id,hx_hypertension,hx_diabetes_type2,hx_diabetes_type1,hx_asthma,hx_copd,hx_heart_failure,"
                + "hx_atrial_fibrillation,hx_ckd,hx_liver_disease,hx_malignancy,hx_obesity,hx_depression,hx_anxiety,"
                + "hx_dementia,hx_epilepsy,hx_hypothyroidism,hx_hyperthyroidism,hx_hiv,hx_coagulopathy,hx_immunosuppressed,"
                + "hx_pregnant,hx_substance_use_disorder,hx_coronary_artery_disease,hx_stroke_prior,hx_peripheral_vascular_disease\n"
                + "TG-TEST0001,1,0,0,0,0,1,0,0,0,1,1,0,1,0,0,1,1,0,0,1,0,0,0,0,0\n"
                + "TG-TEST0002,0,0,0,1,0,1,0,0,1,0,0,0,0,1,0,1,0,0,0,1,0,1,1,0,0\n");

        // quoted fields with embedded commas
        Files.writeString(dir.resolve("chief_complaints.csv"),
                "patient_id,chief_complaint_raw,chief_complaint_system\n"
                + "TG-TEST0001,\"thunderclap headache, worsening with movement\",neurological\n"
                + "TG-TEST0002,\"contraception advice, intermittent\",genitourinary\n");
    }

    private ClinicalDataLoader loader() {
        return new ClinicalDataLoader(jdbc, true, dir.toString());
    }

    @Test
    void loadsAllThreeTables() {
        loader().run();

        assertThat(clinicalRepo.count()).isEqualTo(2);
        assertThat(clinicalRepo.countByDatasetSplit("train")).isEqualTo(2);
        assertThat(historyRepo.count()).isEqualTo(2);
        assertThat(complaintRepo.count()).isEqualTo(2);
    }

    @Test
    void preservesNullableClinicalFieldAndTargetAndSplit() {
        loader().run();

        ClinicalPatientData row2 = clinicalRepo.findByPatientId("TG-TEST0002").orElseThrow();
        assertThat(row2.getSystolicBp()).isNull();      // blank in CSV -> null
        assertThat(row2.getPainScore()).isEqualTo(-1);  // negative value preserved
        assertThat(row2.getTriageAcuity()).isEqualTo(5);
        assertThat(row2.getDatasetSplit()).isEqualTo("train");
    }

    @Test
    void parsesQuotedComplaintAndHistoryFlags() {
        loader().run();

        ChiefComplaint c = complaintRepo.findByPatientId("TG-TEST0001").orElseThrow();
        assertThat(c.getChiefComplaintRaw()).isEqualTo("thunderclap headache, worsening with movement");
        assertThat(c.getChiefComplaintSystem()).isEqualTo("neurological");

        PatientHistory h = historyRepo.findByPatientId("TG-TEST0001").orElseThrow();
        assertThat(h.getHxHypertension()).isEqualTo(1);
        assertThat(h.getHxDiabetesType2()).isEqualTo(0);
    }

    @Test
    void reloadIsIdempotentViaPatientIdUnique() {
        loader().run();
        loader().run(); // second pass must not duplicate

        assertThat(clinicalRepo.count()).isEqualTo(2);
        assertThat(historyRepo.count()).isEqualTo(2);
        assertThat(complaintRepo.count()).isEqualTo(2);
    }

    @Test
    void duplicatePatientIdViolatesUniqueConstraint() {
        loader().run();
        ClinicalPatientData dup = clinicalRepo.findByPatientId("TG-TEST0001").orElseThrow();

        // saving a second managed row with the same patientId must be rejected by the DB
        assertThatThrownBy(() -> jdbc.update(
                "insert into clinical_patient_data (patient_id, dataset_split) values (?, ?)",
                dup.getPatientId(), "train"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
