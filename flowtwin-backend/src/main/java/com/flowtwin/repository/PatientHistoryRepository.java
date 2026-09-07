package com.flowtwin.repository;

import com.flowtwin.model.PatientHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientHistoryRepository extends JpaRepository<PatientHistory, Long> {

    Optional<PatientHistory> findByPatientId(String patientId);

    boolean existsByPatientId(String patientId);
}
