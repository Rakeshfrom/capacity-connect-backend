package com.capacityconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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

    @Value("${DASHSCOPE_MODEL:qwen3.7-plus}")
    private String model;

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

        throw new IllegalArgumentException("Supported resources: PDF, PNG, JPG, JPEG, WEBP");
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (var document = Loader.loadPDF(file.getBytes())) {

            String text = new PDFTextStripper().getText(document);

            if (text != null && !text.isBlank()) {
                return limit(text);
            }

            PDFRenderer renderer = new PDFRenderer(document);
            List<String> images = new ArrayList<>();

            int pages = Math.min(document.getNumberOfPages(), 8);

            for (int i = 0; i < pages; i++) {
                var image = renderer.renderImageWithDPI(i, 120);

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);

                String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
                images.add("data:image/png;base64," + base64);
            }

            if (images.isEmpty()) {
                throw new IllegalArgumentException("Could not read PDF content");
            }

            return extractFromImages(images);
        }
    }

    private String extractImage(MultipartFile file) throws Exception {
        String mime = file.getContentType();

        if (mime == null || !mime.startsWith("image/")) {
            mime = "image/jpeg";
        }

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        return extractFromImages(
                List.of("data:" + mime + ";base64," + base64)
        );
    }

    private String extractFromImages(List<String> images) throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();

        for (String image : images) {
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", image)
            ));
        }

        content.add(Map.of(
                "type", "text",
                "text", """
                        Extract all readable text from these document pages.
                        Preserve headings, paragraphs, lists and important values.
                        Return only the extracted text.
                        Do not summarize.
                        """
        ));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", content
                        )
                ),
                "stream", false,
                "enable_thinking", false
        );

        String raw = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(raw);

        String result = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();

        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException("AI could not extract content from resource");
        }

        return limit(result);
    }

    private String limit(String text) {
        return text.length() > 30000
                ? text.substring(0, 30000)
                : text;
    }
}
