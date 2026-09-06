package com.flowtwin.repository;

import com.flowtwin.model.ClinicalPatientData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalPatientDataRepository extends JpaRepository<ClinicalPatientData, Long> {

    Optional<ClinicalPatientData> findByPatientId(String patientId);

    boolean existsByPatientId(String patientId);

    long countByDatasetSplit(String datasetSplit);

    List<ClinicalPatientData> findByTriageAcuity(Integer triageAcuity);
}
