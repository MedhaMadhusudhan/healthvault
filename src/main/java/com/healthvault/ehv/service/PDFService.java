package com.healthvault.ehv.service;

import com.healthvault.ehv.model.LabTest;
import com.healthvault.ehv.model.TestDetail;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PDFService {

    public byte[] generateLabTestPDF(LabTest labTest) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add Header
            document.add(new Paragraph("HealthVault")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold());

            document.add(new Paragraph("Laboratory Test Report")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16));

            // Add Laboratory Info
            document.add(new Paragraph("\nLaboratory Name: " + labTest.getLaboratoryName())
                    .setFontSize(14));

            // Add Patient Info
            document.add(new Paragraph("Patient Name: " + labTest.getUser().getName())
                    .setFontSize(12));
            document.add(new Paragraph("Patient ID: " + labTest.getUser().getId())
                    .setFontSize(12));

            // Add Test Details Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Add table headers
            table.addHeaderCell("Test Name");
            table.addHeaderCell("Description");

            // Add test details
            for (TestDetail detail : labTest.getTestDetails()) {
                table.addCell(detail.getTestName());
                table.addCell(detail.getDescription() != null ? detail.getDescription() : "");
            }

            document.add(new Paragraph("\n")); // Add some spacing
            document.add(table);

            // Add Footer
            document.add(new Paragraph("\nReport Generated On: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
    
    /**
     * Generate and return a ResponseEntity with the PDF content for a lab test
     * 
     * @param labTest The lab test to generate a PDF for
     * @return ResponseEntity with PDF content
     */
    public ResponseEntity<byte[]> generateLabTestPDFResponse(LabTest labTest) {
        byte[] pdfContent = generateLabTestPDF(labTest);

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
}