package com.healthvault.ehv.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_lab_tests")
public class SharedLabTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "shared_date", nullable = false)
    private LocalDateTime sharedDate;

    @Column(name = "access_expiry")
    private LocalDateTime accessExpiry;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "doctor_reply", columnDefinition = "TEXT")
    private String doctorReply;
    
    @Column(name = "reply_date")
    private LocalDateTime replyDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Default constructor
    public SharedLabTest() {
        this.sharedDate = LocalDateTime.now();
    }

    // Constructor with parameters
    public SharedLabTest(LabTest labTest, User patient, User doctor) {
        this.labTest = labTest;
        this.patient = patient;
        this.doctor = doctor;
        this.sharedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LabTest getLabTest() {
        return labTest;
    }

    public void setLabTest(LabTest labTest) {
        this.labTest = labTest;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
    }

    public LocalDateTime getSharedDate() {
        return sharedDate;
    }

    public void setSharedDate(LocalDateTime sharedDate) {
        this.sharedDate = sharedDate;
    }

    public LocalDateTime getAccessExpiry() {
        return accessExpiry;
    }

    public void setAccessExpiry(LocalDateTime accessExpiry) {
        this.accessExpiry = accessExpiry;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDoctorReply() {
        return doctorReply;
    }
    
    public void setDoctorReply(String doctorReply) {
        this.doctorReply = doctorReply;
    }
    
    public LocalDateTime getReplyDate() {
        return replyDate;
    }
    
    public void setReplyDate(LocalDateTime replyDate) {
        this.replyDate = replyDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Helper method to check if access is expired
    public boolean isExpired() {
        if (accessExpiry == null) {
            return false; // No expiry date set means unlimited access
        }
        
        // Check if the current date/time is after the expiry date
        return LocalDateTime.now().isAfter(accessExpiry);
    }
    
    // Helper method to check if the doctor has replied
    public boolean hasReply() {
        return doctorReply != null && !doctorReply.isEmpty();
    }
} 