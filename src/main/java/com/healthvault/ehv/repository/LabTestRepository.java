package com.healthvault.ehv.repository;

import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {
    List<LabTest> findByUser(User user);
}