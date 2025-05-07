package com.healthvault.ehv.service;

import com.healthvault.ehv.model.Analysis;
import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.repository.AnalysisRepository;
import com.healthvault.ehv.repository.LabTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LabTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(LabTestService.class);

    @Autowired
    private LabTestRepository labTestRepository;
    
    @Autowired
    private AnalysisRepository analysisRepository;

    public LabTest saveLabTest(LabTest labTest) {
        return labTestRepository.save(labTest);
    }

    public List<LabTest> getLabTestsByUser(User user) {
        return labTestRepository.findByUser(user);
    }

    public LabTest getLabTestById(Long id) {
        return labTestRepository.findById(id).orElse(null);
    }
    
    public Optional<LabTest> findById(Long id) {
        return labTestRepository.findById(id);
    }

    @Transactional
    public void deleteLabTest(Long id) {
        try {
            LabTest labTest = labTestRepository.findById(id).orElse(null);
            if (labTest == null) {
                logger.warn("Attempted to delete non-existent lab test with id: {}", id);
                return;
            }
            
            // First delete any associated analysis
            Optional<Analysis> analysis = analysisRepository.findByLabTest(labTest);
            if (analysis.isPresent()) {
                logger.info("Deleting associated analysis for lab test with id: {}", id);
                analysisRepository.delete(analysis.get());
                analysisRepository.flush(); // Force immediate deletion
            }
            
            // Now delete the lab test
            logger.info("Deleting lab test with id: {}", id);
            labTestRepository.deleteById(id);
            labTestRepository.flush(); // Force immediate deletion
            logger.info("Successfully deleted lab test with id: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting lab test with id: {}. Error type: {}. Error message: {}", 
                id, e.getClass().getName(), e.getMessage(), e);
            // Re-throw the exception to be handled by the controller
            throw e;
        }
    }
}