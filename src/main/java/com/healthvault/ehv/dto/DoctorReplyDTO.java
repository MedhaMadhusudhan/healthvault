package com.healthvault.ehv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DoctorReplyDTO {
    @NotNull(message = "Shared lab test ID is required")
    private Long sharedLabTestId;
    
    @NotBlank(message = "Reply message is required")
    @Size(max = 1000, message = "Reply cannot be longer than 1000 characters")
    private String reply;
    
    // Getters and Setters
    public Long getSharedLabTestId() {
        return sharedLabTestId;
    }
    
    public void setSharedLabTestId(Long sharedLabTestId) {
        this.sharedLabTestId = sharedLabTestId;
    }
    
    public String getReply() {
        return reply;
    }
    
    public void setReply(String reply) {
        this.reply = reply;
    }
} 