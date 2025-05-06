package com.healthvault.ehv.service;

import com.healthvault.ehv.model.Analysis;
import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String API_KEY = "AIzaSyAb_Q2scuAn5iwILGmqetjGnDdVtHaGMPY";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public Analysis generateAnalysis(LabTest labTest) {
        // Check if analysis already exists and needs update
        Analysis analysis = analysisRepository.findByLabTest(labTest)
                .orElseGet(() -> {
                    Analysis newAnalysis = new Analysis();
                    newAnalysis.setLabTest(labTest);
                    return newAnalysis;
                });

        // Compile test details into a prompt
        String testDetails = labTest.getTestDetails().stream()
                .map(detail -> String.format("%s: %s", detail.getTestName(), detail.getDescription()))
                .collect(Collectors.joining("\n\n"));

        String prompt = String.format(
            "Analyze the following lab test results and provide key findings. Format your response in exactly 6 points using numbers (1-6). Keep each point brief and clear, focusing on the most important aspects. After the analysis, provide 3 specific recommendations, also numbered.\n\nTest Details:\n%s",
            testDetails
        );

        Map<String, String> analysisResult = analyzeThroughGemini(prompt);
        
        analysis.setAnalysis(analysisResult.get("analysis"));
        analysis.setSuggestions(analysisResult.get("suggestions"));

        return analysisRepository.save(analysis);
    }

    private Map<String, String> analyzeThroughGemini(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", API_KEY);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("text", prompt);
            contents.put("parts", new Object[]{textPart});
            requestBody.put("contents", new Object[]{contents});

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                API_URL,
                request,
                Map.class
            );

            // Parse the response
            String generatedText = extractTextFromResponse(response);
            Map<String, String> result = new HashMap<>();
            String[] sections = generatedText.split("\nSuggestions:");

            // Format analysis points
            String analysisText = sections[0].replaceFirst("(?i)Analysis:", "").trim();
            String[] points = analysisText.split("\\d+\\.");
            StringBuilder formattedAnalysis = new StringBuilder();
            
            for (int i = 1; i < points.length && i <= 6; i++) {
                String point = points[i].trim();
                if (!point.isEmpty()) {
                    formattedAnalysis.append(i).append(". ").append(point).append("\n\n");
                }
            }
            
            // Format suggestions with extra spacing
            String suggestionsText = sections.length > 1 ? sections[1].trim() : "1. Please consult your healthcare provider.\n\n2. Regular follow-up is recommended.\n\n3. Maintain a healthy lifestyle.";
            String[] suggestions = suggestionsText.split("\\d+\\.");
            StringBuilder formattedSuggestions = new StringBuilder();
            
            for (int i = 1; i < suggestions.length && i <= 3; i++) {
                String suggestion = suggestions[i].trim();
                if (!suggestion.isEmpty()) {
                    formattedSuggestions.append(i).append(". ").append(suggestion).append("\n\n");
                }
            }

            result.put("analysis", formattedAnalysis.toString().trim());
            result.put("suggestions", formattedSuggestions.toString().trim());

            return result;
        } catch (Exception e) {
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("analysis", "1. Error analyzing lab results: " + e.getMessage());
            errorResult.put("suggestions", "1. Please consult with your healthcare provider for proper analysis.");
            return errorResult;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            Map<String, Object> candidate = ((java.util.List<Map<String, Object>>) response.get("candidates")).get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            Map<String, Object> part = ((java.util.List<Map<String, Object>>) content.get("parts")).get(0);
            return (String) part.get("text");
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }
}