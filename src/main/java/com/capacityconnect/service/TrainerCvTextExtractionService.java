package com.capacityconnect.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TrainerCvTextExtractionService {

    private final FileStorageService fileStorageService;

    public String extract(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Trainer CV is missing");
        }

        Path path = fileStorageService.load(storageKey);
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

        try {
            String text;

            if (fileName.endsWith(".pdf")) {
                try (var document = Loader.loadPDF(path.toFile())) {
                    text = new PDFTextStripper().getText(document);
                }
            } else if (fileName.endsWith(".docx")) {
                try (InputStream input = Files.newInputStream(path);
                     XWPFDocument document = new XWPFDocument(input);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text = extractor.getText();
                }
            } else if (fileName.endsWith(".doc")) {
                try (InputStream input = Files.newInputStream(path);
                     HWPFDocument document = new HWPFDocument(input);
                     WordExtractor extractor = new WordExtractor(document)) {
                    text = extractor.getText();
                }
            } else {
                throw new IllegalArgumentException("Unsupported trainer CV format");
            }

            text = text == null ? "" : text
                    .replace("\u0000", " ")
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\\n\\n")
                    .trim();

            if (text.isBlank()) {
                throw new IllegalStateException(
                        "Unable to extract readable text from trainer CV"
                );
            }

            return text.substring(0, Math.min(text.length(), 30000));

        } catch (Exception e) {
            if (e instanceof IllegalArgumentException
                    || e instanceof IllegalStateException) {
                throw (RuntimeException) e;
            }

            throw new IllegalStateException(
                    "Unable to extract text from trainer CV", e
            );
        }
    }
}
