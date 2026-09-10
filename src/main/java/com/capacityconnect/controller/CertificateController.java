package com.capacityconnect.controller;

import com.capacityconnect.dto.CertificateResponse;
import com.capacityconnect.dto.CertificateVerificationResponse;
import com.capacityconnect.service.CertificatePdfService;
import com.capacityconnect.service.CertificateService;
import com.capacityconnect.service.CertificateVerificationService;
import com.capacityconnect.repository.CertificateRepository;
import com.capacityconnect.entity.Certificate;
import com.capacityconnect.entity.User;
import org.springframework.security.core.Authentication;
import com.capacityconnect.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificatePdfService certificatePdfService;
    private final CertificateVerificationService certificateVerificationService;
    private final CurrentUserService currentUserService;
    private final CertificateRepository certificateRepository;

    @GetMapping
    public ResponseEntity<List<CertificateResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAll());
    }

    @GetMapping("/trainee/{traineeId}")
    public ResponseEntity<List<CertificateResponse>> getByTrainee(
            @PathVariable Long traineeId) {
        return ResponseEntity.ok(certificateService.getByTrainee(traineeId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates(
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return ResponseEntity.ok(
                certificateService.getByTrainee(currentUser.getId())
        );
    }

    @PostMapping("/issue")
    public ResponseEntity<CertificateResponse> issue(
            @RequestParam Long traineeId,
            @RequestParam Long courseId,
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        boolean isOwner = currentUser.getId().equals(traineeId);

        if (!isAdmin && !isOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot issue a certificate for another trainee");
        }

        if (!isAdmin && currentUser.getRole() != User.Role.TRAINEE) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only trainees can issue their own certificate");
        }

        return ResponseEntity.ok(
                certificateService.issue(traineeId, courseId)
        );
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<CertificateResponse> revoke(
            @PathVariable Long id) {
        return ResponseEntity.ok(certificateService.revoke(id));
    }

    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<CertificateVerificationResponse> verify(
            @PathVariable String certificateNumber) {
        return ResponseEntity.ok(
                certificateVerificationService.verify(certificateNumber)
        );
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> view(
            @PathVariable Long id,
            Authentication authentication) {

        verifyCertificateAccess(id, authentication);

        byte[] pdf = certificatePdfService.generate(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename("certificate-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    private void verifyCertificateAccess(
            Long certificateId,
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Certificate not found"));

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        boolean isOwner = certificate.getTraineeId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this certificate");
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            Authentication authentication) {

        verifyCertificateAccess(id, authentication);

        byte[] pdf = certificatePdfService.generate(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("certificate-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
