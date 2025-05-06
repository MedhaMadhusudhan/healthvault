package com.healthvault.ehv.controller;

import com.healthvault.ehv.model.Analysis;
import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.TestDetail;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.service.AnalysisService;
import com.healthvault.ehv.service.LabTestService;
import com.healthvault.ehv.service.PDFService;
import com.healthvault.ehv.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/lab-tests")
public class LabTestController {

    @Autowired
    private LabTestService labTestService;

    @Autowired
    private UserService userService;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private AnalysisService analysisService;

    @GetMapping("/new")
    public String showNewLabTestForm(Model model) {
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

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        LabTest labTest = labTestService.getLabTestById(id);
        model.addAttribute("labTest", labTest);
        return "lab-tests/edit";
    }

    @PostMapping("/update/{id}")
    public String updateLabTest(@PathVariable Long id, @ModelAttribute LabTest labTest) {
        LabTest existingLabTest = labTestService.getLabTestById(id);
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
    public String deleteLabTest(@PathVariable Long id) {
        labTestService.deleteLabTest(id);
        return "redirect:/dashboard";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadLabTestPDF(@PathVariable Long id) {
        LabTest labTest = labTestService.getLabTestById(id);
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
    public String analyzeLabTest(@PathVariable Long id, Model model) {
        LabTest labTest = labTestService.getLabTestById(id);
        Analysis analysis = analysisService.generateAnalysis(labTest);
        model.addAttribute("analysis", analysis);
        model.addAttribute("labTest", labTest);
        return "lab-tests/analysis";
    }
}