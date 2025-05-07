package com.healthvault.ehv.repository;

import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.SharedLabTest;
import com.healthvault.ehv.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedLabTestRepository extends JpaRepository<SharedLabTest, Long> {
    List<SharedLabTest> findByPatient(User patient);
    List<SharedLabTest> findByDoctor(User doctor);
    List<SharedLabTest> findByPatientAndDoctor(User patient, User doctor);
    List<SharedLabTest> findByPatientAndActiveTrue(User patient);
    List<SharedLabTest> findByDoctorAndActiveTrue(User doctor);
    long countByLabTest(LabTest labTest);
    List<SharedLabTest> findByLabTest(LabTest labTest);
    List<SharedLabTest> findByLabTestAndActiveTrue(LabTest labTest);
} 