package com.capacityconnect.service;

import com.capacityconnect.entity.Certificate;
import com.capacityconnect.repository.CertificateRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class CertificatePdfService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public byte[] generate(Long certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        if (certificate.getStatus() != Certificate.Status.ISSUED) {
            throw new IllegalArgumentException("Certificate is not active");
        }

        var course = courseRepository.findById(certificate.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        var trainee = userRepository.findById(certificate.getTraineeId())
                .orElseThrow(() -> new IllegalArgumentException("Trainee not found"));

        String traineeName = trainee.getFirstName() + " " + trainee.getLastName();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                PDType1Font bold =
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular =
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                content.beginText();
                content.setFont(bold, 24);
                content.newLineAtOffset(145, height - 120);
                content.showText("CAPACITY CONNECT");
                content.endText();

                content.beginText();
                content.setFont(bold, 18);
                content.newLineAtOffset(185, height - 180);
                content.showText("CERTIFICATE OF COMPLETION");
                content.endText();

                content.beginText();
                content.setFont(regular, 12);
                content.newLineAtOffset(180, height - 240);
                content.showText("This is to certify that");
                content.endText();

                content.beginText();
                content.setFont(bold, 22);
                content.newLineAtOffset(150, height - 290);
                content.showText(traineeName);
                content.endText();

                content.beginText();
                content.setFont(regular, 12);
                content.newLineAtOffset(150, height - 340);
                content.showText("has successfully completed the course");
                content.endText();

                content.beginText();
                content.setFont(bold, 16);
                content.newLineAtOffset(120, height - 380);
                content.showText(course.getTitle());
                content.endText();

                content.beginText();
                content.setFont(regular, 11);
                content.newLineAtOffset(175, height - 430);
                content.showText("Certificate Number: " + certificate.getCertificateNumber());
                content.endText();

                content.beginText();
                content.setFont(regular, 11);
                content.newLineAtOffset(215, height - 455);
                content.showText("Issued: " + certificate.getIssuedAt().toLocalDate());
                content.endText();

                content.beginText();
                content.setFont(bold, 11);
                content.newLineAtOffset(245, 100);
                content.showText("Ministry of Earth Sciences / IMD");
                content.endText();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate certificate PDF", e);
        }
    }
}
