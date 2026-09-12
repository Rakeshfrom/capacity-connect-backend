package com.capacityconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AiResourceService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${DASHSCOPE_API_KEY:}")
    private String apiKey;

    @Value("${DASHSCOPE_BASE_URL:https://dashscope-intl.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    public String extractText(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resource file is required");
        }

        String name = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase();

        if (name.endsWith(".pdf")) {
            return extractPdf(file);
        }

        if (name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".webp")) {
            return extractImage(file);
        }

        throw new IllegalArgumentException(
                "Supported resources: PDF, PNG, JPG, JPEG, WEBP"
        );
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (var document = Loader.loadPDF(file.getBytes())) {

            String text = new PDFTextStripper().getText(document);

            if (text != null && !text.isBlank()) {
                return limit(text);
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder result = new StringBuilder();

            int pages = Math.min(document.getNumberOfPages(), 20);

            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 90);

                String pageText = extractImageWithOCR(image);

                if (pageText != null && !pageText.isBlank()) {
                    result.append("\n\n--- Page ")
                          .append(i + 1)
                          .append(" ---\n\n")
                          .append(pageText);
                }
            }

            if (result.isEmpty()) {
                throw new IllegalArgumentException(
                        "Could not extract readable content from PDF"
                );
            }

            return limit(result.toString());
        }
    }

    public String extractTextFromUrl(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Resource URL is required");
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported");
        }

        String text = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get()
                .body()
                .text();

        if (text.isBlank()) {
            throw new IllegalArgumentException("Could not extract readable content from URL");
        }

        return limit(text);
    }

    private String extractImage(MultipartFile file) throws Exception {
        BufferedImage image = ImageIO.read(file.getInputStream());

        if (image == null) {
            throw new IllegalArgumentException("Invalid image resource");
        }

        return limit(extractImageWithOCR(image));
    }

    private String extractImageWithOCR(BufferedImage image) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ImageIO.write(image, "jpg", output);

        String base64 = Base64.getEncoder()
                .encodeToString(output.toByteArray());

        Map<String, Object> content = Map.of(
                "type", "image_url",
                "image_url", Map.of(
                        "url", "data:image/jpeg;base64," + base64
                )
        );

        Map<String, Object> body = Map.of(
                "model", "qwen-vl-ocr-2025-11-20",
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        content,
                                        Map.of(
                                                "type", "text",
                                                "text", """
                                                        Extract all readable text from this document image.
                                                        Preserve headings, paragraphs, lists, numbers and important values.
                                                        Return only the extracted text.
                                                        Do not summarize.
                                                        """
                                        )
                                )
                        )
                ),
                "stream", false
        );

        String raw = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(raw);

        JsonNode responseContent = root.path("choices")
                .path(0)
                .path("message")
                .path("content");

        String result;

        if (responseContent.isTextual()) {
            result = responseContent.asText();
        } else if (responseContent.isArray()) {
            StringBuilder extracted = new StringBuilder();

            for (JsonNode item : responseContent) {
                if (item.has("text")) {
                    extracted.append(item.path("text").asText());
                }
            }

            result = extracted.toString();
        } else {
            result = "";
        }

        if (result.isBlank()) {
            throw new IllegalArgumentException(
                    "OCR returned empty content"
            );
        }

        return result;
    }

    private String limit(String text) {
        return text.length() > 30000
                ? text.substring(0, 30000)
                : text;
    }
}
