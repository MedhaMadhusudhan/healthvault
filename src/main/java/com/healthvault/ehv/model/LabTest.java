package com.healthvault.ehv.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_tests")
public class LabTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String laboratoryName;

    @OneToMany(mappedBy = "labTest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TestDetail> testDetails = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Constructors
    public LabTest() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLaboratoryName() {
        return laboratoryName;
    }

    public void setLaboratoryName(String laboratoryName) {
        this.laboratoryName = laboratoryName;
    }

    public List<TestDetail> getTestDetails() {
        return testDetails;
    }

    public void setTestDetails(List<TestDetail> testDetails) {
        this.testDetails.clear();
        if (testDetails != null) {
            testDetails.forEach(this::addTestDetail);
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Helper method to add test details
    public void addTestDetail(TestDetail testDetail) {
        testDetails.add(testDetail);
        testDetail.setLabTest(this);
    }

    public void removeTestDetail(TestDetail testDetail) {
        testDetails.remove(testDetail);
        testDetail.setLabTest(null);
    }
}