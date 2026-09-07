package com.flowtwin.repository;

import com.flowtwin.model.ChiefComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChiefComplaintRepository extends JpaRepository<ChiefComplaint, Long> {

    Optional<ChiefComplaint> findByPatientId(String patientId);

    boolean existsByPatientId(String patientId);
}
