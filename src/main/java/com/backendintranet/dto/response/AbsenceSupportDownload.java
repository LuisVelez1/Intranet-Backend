package com.backendintranet.dto.response;

import org.springframework.core.io.Resource;

public record AbsenceSupportDownload(
        Resource resource,
        String contentType,
        String filename) {}
