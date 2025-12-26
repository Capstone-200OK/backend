package com.example.demo.controller;

import com.example.demo.dto.ThumbnailUpdateRequestDTO;
import com.example.demo.service.InternalThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class ThumbnailInternalController {

    private final InternalThumbnailService internalThumbnailService;

    @Value("${internal.token}")
    private String internalToken;

    @PatchMapping("/{fileId}/thumbnail")
    public ResponseEntity<Void> updateThumbnail(
            @PathVariable Long fileId,
            @RequestHeader("X-INTERNAL-TOKEN") String token,
            @RequestBody ThumbnailUpdateRequestDTO request
    ) {
        if (!internalToken.equals(token)) {
            return ResponseEntity.status(401).build();
        }

        internalThumbnailService.updateFileThumbnail(fileId, request.getThumbnailUrl());
        return ResponseEntity.ok().build();
    }
}
