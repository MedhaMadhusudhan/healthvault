package com.healthvault.ehv.service;

import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.repository.LabTestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LabTestService {

    @Autowired
    private LabTestRepository labTestRepository;

    public LabTest saveLabTest(LabTest labTest) {
        return labTestRepository.save(labTest);
    }

    public List<LabTest> getLabTestsByUser(User user) {
        return labTestRepository.findByUser(user);
    }

    public LabTest getLabTestById(Long id) {
        return labTestRepository.findById(id).orElse(null);
    }

    public void deleteLabTest(Long id) {
        labTestRepository.deleteById(id);
    }
}