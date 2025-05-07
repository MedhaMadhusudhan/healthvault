package com.healthvault.ehv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ShareLabTestDTO {
    @NotNull(message = "Lab test ID is required")
    private Long labTestId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @Size(max = 1000, message = "Message cannot be longer than 1000 characters")
    private String message;

    private LocalDateTime accessExpiry;

    // Getters and Setters
    public Long getLabTestId() {
        return labTestId;
    }

    public void setLabTestId(Long labTestId) {
        this.labTestId = labTestId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getAccessExpiry() {
        return accessExpiry;
    }

    public void setAccessExpiry(LocalDateTime accessExpiry) {
        this.accessExpiry = accessExpiry;
    }
} 