package com.example.demo.service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Template s3Template;

    public String upload(MultipartFile multipartFile, String bucketName, String dirName) {
        try {
            String originalFilename = multipartFile.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";

            String fileName = dirName + "/" + UUID.randomUUID() + ext;

            s3Template.upload(bucketName, fileName, multipartFile.getInputStream());

            return "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }
}
