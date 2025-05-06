package com.healthvault.ehv.repository;

import com.healthvault.ehv.model.Analysis;
import com.healthvault.ehv.model.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Optional<Analysis> findByLabTest(LabTest labTest);
}