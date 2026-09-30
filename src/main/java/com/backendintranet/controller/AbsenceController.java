package com.backendintranet.controller;

import com.backendintranet.dto.request.AbsenceCreateRequest;
import com.backendintranet.dto.request.AbsenceDecisionRequest;
import com.backendintranet.dto.response.AbsenceResponse;
import com.backendintranet.dto.response.AbsenceSupportDownload;
import com.backendintranet.service.AbsenceService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService service;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AbsenceResponse> create(
            @Valid @RequestBody AbsenceCreateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AbsenceResponse> createMultipart(
            @RequestPart("data")
            @Valid
            AbsenceCreateRequest request,
            @RequestPart(
                    value = "file",
                    required = false)
            MultipartFile file) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(
                                request,
                                file));
    }

    @GetMapping("/{id}/support")
    public ResponseEntity<Resource> support(
            @PathVariable String id) {

        AbsenceSupportDownload download =
                service.getSupport(id);

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.parseMediaType(
                                download.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(
                                        download.filename())
                                .build()
                                .toString())
                .body(
                        download.resource());
    }

    @GetMapping("/my")
    public List<AbsenceResponse> my() {
        return service.findMyRequests();
    }

    @GetMapping("/pending")
    public List<AbsenceResponse> pending() {
        return service.findPendingApprovals();
    }

    @GetMapping("/reporting")
    public List<AbsenceResponse> reporting() {
        return service.findAllForReporting();
    }

    @GetMapping("/capabilities")
    public Map<String, Boolean> capabilities() {
        return service.getCapabilities();
    }

    @PutMapping("/{id}/approve")
    public AbsenceResponse approve(
            @PathVariable String id,
            @Valid @RequestBody AbsenceDecisionRequest request) {

        return service.approve(id, request);
    }

    @PutMapping("/{id}/reject")
    public AbsenceResponse reject(
            @PathVariable String id,
            @Valid @RequestBody AbsenceDecisionRequest request) {

        return service.reject(id, request);
    }
}
