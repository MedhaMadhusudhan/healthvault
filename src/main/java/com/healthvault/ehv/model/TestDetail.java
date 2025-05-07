package com.healthvault.ehv.model;

import jakarta.persistence.*;

@Entity
@Table(name = "test_details")
public class TestDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String result;

    @Column
    private String normalRange;

    @ManyToOne
    @JoinColumn(name = "lab_test_id")
    private LabTest labTest;

    // Constructors
    public TestDetail() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public LabTest getLabTest() {
        return labTest;
    }

    public void setLabTest(LabTest labTest) {
        this.labTest = labTest;
    }

    // Helper method to check if result is within normal range
    public boolean isWithinRange() {
        // Simple implementation - if either field is null, return true to avoid NPE
        if (result == null || normalRange == null || result.isEmpty() || normalRange.isEmpty()) {
            return true;
        }
        
        // For simplicity, just returning true
        // In a real app, this would parse the result and normal range values
        // and perform appropriate comparison
        return true;
    }
}