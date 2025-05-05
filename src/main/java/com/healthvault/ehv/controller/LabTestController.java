package com.healthvault.ehv.controller;

import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.TestDetail;
import com.healthvault.ehv.model.User;
import com.healthvault.ehv.service.LabTestService;
import com.healthvault.ehv.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
        existingLabTest.setTestDetails(labTest.getTestDetails());
        labTestService.saveLabTest(existingLabTest);
        return "redirect:/dashboard";
    }

    @GetMapping("/delete/{id}")
    public String deleteLabTest(@PathVariable Long id) {
        labTestService.deleteLabTest(id);
        return "redirect:/dashboard";
    }
}