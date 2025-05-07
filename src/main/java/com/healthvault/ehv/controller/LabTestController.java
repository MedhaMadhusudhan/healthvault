package com.healthvault.ehv.controller;

import com.healthvault.ehv.model.Analysis;
import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.TestDetail;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.security.CustomUserDetails;
import com.healthvault.ehv.service.AnalysisService;
import com.healthvault.ehv.service.LabTestService;
import com.healthvault.ehv.service.PDFService;
import com.healthvault.ehv.service.UserService;
import com.healthvault.ehv.service.SharedLabTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lab-tests")
public class LabTestController {

    private static final Logger logger = LoggerFactory.getLogger(LabTestController.class);

    @Autowired
    private LabTestService labTestService;

    @Autowired
    private UserService userService;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SharedLabTestService sharedLabTestService;

    @GetMapping("/new")
    public String showNewLabTestForm(Model model, Authentication authentication) {
        model.addAttribute("labTest", new LabTest());
        model.addAttribute("testDetail", new TestDetail());
        
        return "lab-tests/create";
    }

    @PostMapping("/save")
    public String saveLabTest(@ModelAttribute LabTest labTest, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        labTest.setUser(user);

        // Ensure bidirectional relationship is properly set
        if (labTest.getTestDetails() != null) {
            labTest.getTestDetails().forEach(detail -> detail.setLabTest(labTest));
        }

        labTestService.saveLabTest(labTest);
        return "redirect:/dashboard";
    }
    
    @GetMapping("/view/{id}")
    public String viewLabTest(@PathVariable Long id, Model model, Authentication authentication) {
        logger.debug("Viewing lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        LabTest labTest = labTestService.getLabTestById(id);
        if (labTest == null) {
            logger.error("Lab test not found with ID: {}", id);
            return "redirect:/dashboard";
        }
        
        // Check if the user has access to this lab test
        if (!labTest.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have access to lab test {}", currentUser.getId(), id);
            return "redirect:/dashboard";
        }
        
        model.addAttribute("labTest", labTest);
        model.addAttribute("currentUser", currentUser);
        
        // Check if the user is a patient to enable sharing functionality
        boolean canShare = currentUser.getRole() == User.Role.PATIENT;
        model.addAttribute("canShare", canShare);
        
        return "lab-tests/view";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        logger.debug("Showing edit form for lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        LabTest labTest = labTestService.getLabTestById(id);
        if (labTest == null) {
            logger.error("Lab test not found with ID: {}", id);
            return "redirect:/dashboard";
        }
        
        // Check if the user has access to this lab test
        if (!labTest.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have access to lab test {}", currentUser.getId(), id);
            return "redirect:/dashboard";
        }
        
        model.addAttribute("labTest", labTest);
        
        return "lab-tests/edit";
    }

    @PostMapping("/update/{id}")
    public String updateLabTest(@PathVariable Long id, @ModelAttribute LabTest labTest, Authentication authentication) {
        logger.debug("Updating lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        LabTest existingLabTest = labTestService.getLabTestById(id);
        if (existingLabTest == null) {
            logger.error("Lab test not found with ID: {}", id);
            return "redirect:/dashboard";
        }
        
        // Check if the user has access to this lab test
        if (!existingLabTest.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have access to lab test {}", currentUser.getId(), id);
            return "redirect:/dashboard";
        }
        
        existingLabTest.setLaboratoryName(labTest.getLaboratoryName());

        // Ensure bidirectional relationship is properly set for each test detail
        if (labTest.getTestDetails() != null) {
            labTest.getTestDetails().forEach(detail -> detail.setLabTest(existingLabTest));
        }
        existingLabTest.setTestDetails(labTest.getTestDetails());

        labTestService.saveLabTest(existingLabTest);
        return "redirect:/dashboard";
    }

    @GetMapping("/delete/{id}")
    public String deleteLabTest(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        logger.debug("Deleting lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        LabTest existingLabTest = labTestService.getLabTestById(id);
        if (existingLabTest == null) {
            logger.error("Lab test not found with ID: {}", id);
            return "redirect:/dashboard";
        }
        
        // Check if the user has access to this lab test
        if (!existingLabTest.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have access to lab test {}", currentUser.getId(), id);
            return "redirect:/dashboard";
        }
        
        // Check if the lab test is shared with any doctors
        if (sharedLabTestService.isLabTestShared(id)) {
            logger.warn("Cannot delete lab test with ID: {} as it is shared with doctors", id);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "This lab test cannot be deleted because it is currently shared with one or more doctors. Please revoke all shared access first.");
            return "redirect:/dashboard";
        }
        
        try {
            labTestService.deleteLabTest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Lab test deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting lab test with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "An error occurred while deleting the lab test. Please try again later.");
        }
        
        return "redirect:/dashboard";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadLabTestPDF(@PathVariable Long id, Authentication authentication) {
        logger.debug("Downloading PDF for lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        LabTest labTest = labTestService.getLabTestById(id);
        if (labTest == null) {
            logger.error("Lab test not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        // Check if the user has access to this lab test
        if (!labTest.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have access to lab test {}", currentUser.getId(), id);
            return ResponseEntity.notFound().build();
        }
        
        byte[] pdfContent = pdfService.generateLabTestPDF(labTest);

        String filename = "LabTest_" + labTest.getLaboratoryName().replaceAll("\\s+", "_") + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/analyze/{id}")
    public String analyzeLabTest(@PathVariable Long id, Model model, Authentication authentication) {
        logger.debug("Analyzing lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        LabTest labTest = labTestService.getLabTestById(id);
        if (labTest == null) {
            logger.error("Lab test not found with ID: {}", id);
            return "redirect:/dashboard";
        }
        
        // Check if the user has access to this lab test
        if (!labTest.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have access to lab test {}", currentUser.getId(), id);
            return "redirect:/dashboard";
        }
        
        Analysis analysis = analysisService.generateAnalysis(labTest);
        model.addAttribute("analysis", analysis);
        model.addAttribute("labTest", labTest);
        
        return "lab-tests/analysis";
    }
}