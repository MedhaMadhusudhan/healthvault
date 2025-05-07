package com.healthvault.ehv.controller;

import com.healthvault.ehv.dto.DoctorReplyDTO;
import com.healthvault.ehv.dto.ShareLabTestDTO;
import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.SharedLabTest;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.security.CustomUserDetails;
import com.healthvault.ehv.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/share")
public class SharedLabTestController {

    private static final Logger logger = LoggerFactory.getLogger(SharedLabTestController.class);

    @Autowired
    private SharedLabTestService sharedLabTestService;

    @Autowired
    private UserService userService;

    @Autowired
    private LabTestService labTestService;
    
    @Autowired
    private PDFService pdfService;

    /**
     * Show form to share a lab test
     */
    @GetMapping("/lab-test/{id}")
    public String showShareLabTestForm(@PathVariable Long id, Model model, Authentication authentication) {
        logger.debug("Showing form to share lab test with ID: {}", id);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        // Check if the user is a patient
        if (currentUser.getRole() != User.Role.PATIENT) {
            logger.error("Non-patient user tried to share lab test: {}", currentUser.getUsername());
            return "redirect:/dashboard";
        }
        
        // Get the lab test
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
        
        // Get all doctors
        List<User> doctors = userService.findAllDoctors();
        
        // Create DTO for the form
        ShareLabTestDTO shareLabTestDTO = new ShareLabTestDTO();
        shareLabTestDTO.setLabTestId(id);
        
        model.addAttribute("labTest", labTest);
        model.addAttribute("doctors", doctors);
        model.addAttribute("shareLabTestDTO", shareLabTestDTO);
        
        return "share/share-lab-test";
    }

    /**
     * Process lab test sharing
     */
    @PostMapping("/lab-test")
    public String shareLabTest(@Valid @ModelAttribute ShareLabTestDTO shareLabTestDTO, 
                               BindingResult bindingResult,
                               @RequestParam(name = "duration", required = false) String duration,
                               @RequestParam(name = "expiryDate", required = false) String expiryDate,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        logger.debug("Processing lab test sharing request");
        
        if (bindingResult.hasErrors()) {
            return "share/share-lab-test";
        }
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User patient = userService.findByUsername(userDetails.getUsername());
            
            // Handle expiry date if duration is limited
            if ("limited".equals(duration) && expiryDate != null && !expiryDate.isEmpty()) {
                logger.debug("Setting limited access with expiry date: {}", expiryDate);
                try {
                    // Parse the date in format yyyy-MM-dd and set the time to end of day
                    LocalDateTime expiry = LocalDate.parse(expiryDate).atTime(23, 59, 59);
                    shareLabTestDTO.setAccessExpiry(expiry);
                } catch (Exception e) {
                    logger.error("Error parsing expiry date: {}", expiryDate, e);
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid expiry date format");
                    return "redirect:/share/lab-test/" + shareLabTestDTO.getLabTestId();
                }
            } else {
                // Unlimited access
                shareLabTestDTO.setAccessExpiry(null);
            }
            
            // Share the lab test
            SharedLabTest sharedLabTest = sharedLabTestService.shareLabTest(patient, shareLabTestDTO);
            
            logger.info("Lab test shared successfully: {}", sharedLabTest.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Lab test shared successfully!");
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            logger.error("Error sharing lab test", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/share/lab-test/" + shareLabTestDTO.getLabTestId();
        }
    }

    /**
     * View all lab tests shared by the current patient
     */
    @GetMapping("/my-shared-tests")
    public String viewSharedTests(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        if (currentUser.getRole() != User.Role.PATIENT) {
            return "redirect:/dashboard";
        }
        
        List<SharedLabTest> sharedLabTests = sharedLabTestService.getSharedLabTestsByPatient(currentUser);
        model.addAttribute("sharedLabTests", sharedLabTests);
        
        return "share/my-shared-tests";
    }

    /**
     * View all lab tests shared with the current doctor
     */
    @GetMapping("/shared-with-me")
    public String viewSharedWithMe(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername());
        
        if (currentUser.getRole() != User.Role.DOCTOR) {
            return "redirect:/dashboard";
        }
        
        List<SharedLabTest> sharedLabTests = sharedLabTestService.getActiveSharedLabTestsByDoctor(currentUser);
        model.addAttribute("sharedLabTests", sharedLabTests);
        
        return "share/shared-with-me";
    }

    /**
     * Show form to add a reply to a shared lab test
     */
    @GetMapping("/reply/{id}")
    public String showReplyForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.debug("Showing form to reply to shared lab test with ID: {}", id);
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userService.findByUsername(userDetails.getUsername());
            
            // Only doctors can reply
            if (currentUser.getRole() != User.Role.DOCTOR) {
                logger.error("Non-doctor user tried to reply to shared lab test: {}", currentUser.getUsername());
                redirectAttributes.addFlashAttribute("errorMessage", "Only doctors can reply to shared lab tests");
                return "redirect:/dashboard";
            }
            
            Optional<SharedLabTest> sharedLabTestOpt = sharedLabTestService.getSharedLabTestById(id);
            if (sharedLabTestOpt.isEmpty()) {
                logger.error("Shared lab test not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Shared lab test not found");
                return "redirect:/share/shared-with-me";
            }
            
            SharedLabTest sharedLabTest = sharedLabTestOpt.get();
            
            // Check if the current doctor is the intended recipient
            if (!sharedLabTest.getDoctor().getId().equals(currentUser.getId())) {
                logger.error("Doctor {} does not have access to shared lab test {}", currentUser.getUsername(), id);
                redirectAttributes.addFlashAttribute("errorMessage", "You do not have access to this shared lab test");
                return "redirect:/share/shared-with-me";
            }
            
            // Check if the shared test is still active
            if (!sharedLabTest.isActive()) {
                logger.error("Shared lab test {} is no longer active", id);
                redirectAttributes.addFlashAttribute("errorMessage", "This shared lab test is no longer active");
                return "redirect:/share/shared-with-me";
            }
            
            // Create and populate the DTO for the form
            DoctorReplyDTO replyDTO = new DoctorReplyDTO();
            replyDTO.setSharedLabTestId(id);
            
            // If there's already a reply, populate it in the form
            if (sharedLabTest.getDoctorReply() != null) {
                replyDTO.setReply(sharedLabTest.getDoctorReply());
            }
            
            model.addAttribute("sharedLabTest", sharedLabTest);
            model.addAttribute("replyDTO", replyDTO);
            
            return "share/reply-form";
        } catch (Exception e) {
            logger.error("Error showing reply form for shared lab test with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "redirect:/share/shared-with-me";
        }
    }
    
    /**
     * Process doctor's reply to a shared lab test
     */
    @PostMapping("/reply")
    public String processReply(@Valid @ModelAttribute DoctorReplyDTO replyDTO, 
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication,
                               Model model) {
        logger.debug("Processing reply to shared lab test with ID: {}", replyDTO.getSharedLabTestId());
        
        if (bindingResult.hasErrors()) {
            // If there are validation errors, re-show the form
            Optional<SharedLabTest> sharedLabTestOpt = sharedLabTestService.getSharedLabTestById(replyDTO.getSharedLabTestId());
            if (sharedLabTestOpt.isPresent()) {
                model.addAttribute("sharedLabTest", sharedLabTestOpt.get());
            }
            return "share/reply-form";
        }
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User doctor = userService.findByUsername(userDetails.getUsername());
            
            // Add the reply
            SharedLabTest updatedSharedLabTest = sharedLabTestService.addDoctorReply(doctor, replyDTO);
            
            logger.info("Reply added successfully to shared lab test with ID: {}", updatedSharedLabTest.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Your reply has been saved successfully!");
            return "redirect:/share/shared-with-me";
            
        } catch (Exception e) {
            logger.error("Error adding reply to shared lab test", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/share/reply/" + replyDTO.getSharedLabTestId();
        }
    }
    
    /**
     * View details of a shared lab test
     */
    @GetMapping("/view/{id}")
    public String viewSharedLabTest(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.debug("Viewing shared lab test with ID: {}", id);
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userService.findByUsername(userDetails.getUsername());
            
            Optional<SharedLabTest> sharedLabTestOpt = sharedLabTestService.getSharedLabTestById(id);
            if (sharedLabTestOpt.isEmpty()) {
                logger.error("Shared lab test not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Shared lab test not found");
                return "redirect:" + (currentUser.getRole() == User.Role.PATIENT ? "/share/my-shared-tests" : "/share/shared-with-me");
            }
            
            SharedLabTest sharedLabTest = sharedLabTestOpt.get();
            
            // Check if the current user is either the patient who shared or the doctor it's shared with
            boolean isPatientOwner = sharedLabTest.getPatient().getId().equals(currentUser.getId());
            boolean isDoctorWithAccess = sharedLabTest.getDoctor().getId().equals(currentUser.getId());
            
            if (!isPatientOwner && !isDoctorWithAccess) {
                logger.error("User {} does not have access to shared lab test {}", currentUser.getUsername(), id);
                redirectAttributes.addFlashAttribute("errorMessage", "You do not have access to this shared lab test");
                return "redirect:" + (currentUser.getRole() == User.Role.PATIENT ? "/share/my-shared-tests" : "/share/shared-with-me");
            }
            
            model.addAttribute("sharedLabTest", sharedLabTest);
            model.addAttribute("currentUser", currentUser);
            
            return "share/view-shared-test";
        } catch (Exception e) {
            logger.error("Error viewing shared lab test with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while trying to view the shared lab test: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Revoke access to a shared lab test
     */
    @GetMapping("/revoke/{id}")
    public String revokeAccess(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User patient = userService.findByUsername(userDetails.getUsername());
            
            sharedLabTestService.revokeAccess(id, patient);
            
            redirectAttributes.addFlashAttribute("successMessage", "Access has been revoked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/share/my-shared-tests";
    }
    
    /**
     * Download a shared lab test PDF
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadSharedLabTestPDF(@PathVariable Long id, Authentication authentication) {
        logger.debug("Downloading PDF for shared lab test with ID: {}", id);
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userService.findByUsername(userDetails.getUsername());
            
            Optional<SharedLabTest> sharedLabTestOpt = sharedLabTestService.getSharedLabTestById(id);
            if (!sharedLabTestOpt.isPresent()) {
                logger.error("Shared lab test not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            SharedLabTest sharedLabTest = sharedLabTestOpt.get();
            
            // Check if the current user is either the patient who shared or the doctor it's shared with
            boolean isPatientOwner = sharedLabTest.getPatient().getId().equals(currentUser.getId());
            boolean isDoctorWithAccess = sharedLabTest.getDoctor().getId().equals(currentUser.getId()) && sharedLabTest.isActive();
            
            if (!isPatientOwner && !isDoctorWithAccess) {
                logger.error("User {} does not have access to download shared lab test {}", currentUser.getUsername(), id);
                return ResponseEntity.status(403).build(); // Forbidden
            }
            
            return pdfService.generateLabTestPDFResponse(sharedLabTest.getLabTest());
        } catch (Exception e) {
            logger.error("Error downloading shared lab test PDF with ID: {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }
} 