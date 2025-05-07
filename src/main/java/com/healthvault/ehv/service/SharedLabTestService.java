package com.healthvault.ehv.service;

import com.healthvault.ehv.dto.DoctorReplyDTO;
import com.healthvault.ehv.dto.ShareLabTestDTO;
import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.SharedLabTest;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.repository.SharedLabTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SharedLabTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(SharedLabTestService.class);

    @Autowired
    private SharedLabTestRepository sharedLabTestRepository;

    @Autowired
    private LabTestService labTestService;

    @Autowired
    private UserService userService;

    /**
     * Share a lab test with a doctor
     * 
     * @param patient The patient sharing the lab test
     * @param shareLabTestDTO The data for sharing the lab test
     * @return The created SharedLabTest object
     */
    public SharedLabTest shareLabTest(User patient, ShareLabTestDTO shareLabTestDTO) {
        if (patient.getRole() != User.Role.PATIENT) {
            throw new RuntimeException("Only patients can share lab tests");
        }

        // Get lab test
        LabTest labTest = labTestService.getLabTestById(shareLabTestDTO.getLabTestId());
        if (labTest == null) {
            throw new RuntimeException("Lab test not found");
        }

        // Ensure the lab test belongs to the patient
        if (!labTest.getUser().getId().equals(patient.getId())) {
            throw new RuntimeException("You can only share your own lab tests");
        }

        // Get doctor
        User doctor = userService.findById(shareLabTestDTO.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        // Ensure the recipient is a doctor
        if (doctor.getRole() != User.Role.DOCTOR) {
            throw new RuntimeException("Lab tests can only be shared with doctors");
        }

        // Create shared lab test
        SharedLabTest sharedLabTest = new SharedLabTest(labTest, patient, doctor);
        sharedLabTest.setMessage(shareLabTestDTO.getMessage());
        sharedLabTest.setAccessExpiry(shareLabTestDTO.getAccessExpiry());

        // Save and return
        return sharedLabTestRepository.save(sharedLabTest);
    }

    /**
     * Get all lab tests shared by a patient
     * 
     * @param patient The patient who shared the lab tests
     * @return List of shared lab tests
     */
    public List<SharedLabTest> getSharedLabTestsByPatient(User patient) {
        return sharedLabTestRepository.findByPatient(patient);
    }

    /**
     * Get all active lab tests shared by a patient
     * 
     * @param patient The patient who shared the lab tests
     * @return List of active shared lab tests
     */
    public List<SharedLabTest> getActiveSharedLabTestsByPatient(User patient) {
        return sharedLabTestRepository.findByPatientAndActiveTrue(patient);
    }

    /**
     * Get all lab tests shared with a doctor
     * 
     * @param doctor The doctor with whom lab tests are shared
     * @return List of shared lab tests
     */
    public List<SharedLabTest> getSharedLabTestsByDoctor(User doctor) {
        return sharedLabTestRepository.findByDoctor(doctor);
    }

    /**
     * Get all active lab tests shared with a doctor
     * 
     * @param doctor The doctor with whom lab tests are shared
     * @return List of active shared lab tests
     */
    public List<SharedLabTest> getActiveSharedLabTestsByDoctor(User doctor) {
        return sharedLabTestRepository.findByDoctorAndActiveTrue(doctor);
    }

    /**
     * Get a shared lab test by ID
     * 
     * @param id The ID of the shared lab test
     * @return The shared lab test, if found
     */
    public Optional<SharedLabTest> getSharedLabTestById(Long id) {
        return sharedLabTestRepository.findById(id);
    }

    /**
     * Revoke access to a shared lab test
     * 
     * @param id The ID of the shared lab test
     * @param patient The patient who shared the lab test
     */
    public void revokeAccess(Long id, User patient) {
        Optional<SharedLabTest> sharedLabTestOpt = sharedLabTestRepository.findById(id);
        
        if (sharedLabTestOpt.isPresent()) {
            SharedLabTest sharedLabTest = sharedLabTestOpt.get();
            
            if (!sharedLabTest.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("You can only revoke access to lab tests you have shared");
            }
            
            sharedLabTest.setActive(false);
            sharedLabTestRepository.save(sharedLabTest);
            logger.info("Access revoked for shared lab test with ID: {}", id);
        } else {
            throw new RuntimeException("Shared lab test not found");
        }
    }

    /**
     * Check if a lab test is shared with any doctors
     * 
     * @param labTestId The ID of the lab test
     * @return true if the lab test is shared with any doctors, false otherwise
     */
    public boolean isLabTestShared(Long labTestId) {
        LabTest labTest = labTestService.getLabTestById(labTestId);
        if (labTest == null) {
            return false;
        }
        
        // Check for active shared records for this lab test
        List<SharedLabTest> activeShares = sharedLabTestRepository.findByLabTestAndActiveTrue(labTest);
        return !activeShares.isEmpty();
    }

    /**
     * Add a doctor's reply to a shared lab test
     * 
     * @param doctor The doctor adding the reply
     * @param replyDTO The DTO containing the reply data
     * @return The updated SharedLabTest object
     */
    public SharedLabTest addDoctorReply(User doctor, DoctorReplyDTO replyDTO) {
        if (doctor.getRole() != User.Role.DOCTOR) {
            throw new RuntimeException("Only doctors can reply to shared lab tests");
        }

        Optional<SharedLabTest> sharedLabTestOpt = sharedLabTestRepository.findById(replyDTO.getSharedLabTestId());
        if (sharedLabTestOpt.isEmpty()) {
            throw new RuntimeException("Shared lab test not found");
        }

        SharedLabTest sharedLabTest = sharedLabTestOpt.get();
        
        // Check if the doctor is the intended recipient
        if (!sharedLabTest.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("You can only reply to lab tests shared with you");
        }
        
        // Check if the shared test is still active
        if (!sharedLabTest.isActive()) {
            throw new RuntimeException("This shared lab test is no longer active");
        }
        
        // Add the reply
        sharedLabTest.setDoctorReply(replyDTO.getReply());
        sharedLabTest.setReplyDate(LocalDateTime.now());
        
        // Save and return
        logger.info("Doctor {} added reply to shared lab test {}", doctor.getUsername(), sharedLabTest.getId());
        return sharedLabTestRepository.save(sharedLabTest);
    }
} 