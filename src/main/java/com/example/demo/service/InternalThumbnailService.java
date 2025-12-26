package com.example.demo.service;

import com.example.demo.entity.File;
import com.example.demo.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalThumbnailService {

    private final FileRepository fileRepository;

    @Transactional
    public void updateFileThumbnail(Long fileId, String thumbnailUrl) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        file.setFileThumbUrl(thumbnailUrl);
    }
}
