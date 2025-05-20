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

    /**
     * S3에 파일을 업로드하고 해당 파일의 URL을 반환
     *
     * @param multipartFile 업로드할 파일
     * @param bucketName    S3 버킷 이름
     * @param dirName       업로드할 디렉토리 경로
     * @return 업로드된 파일의 S3 URL
     */
    public String upload(MultipartFile multipartFile, String bucketName, String dirName) {
        try {
            // 파일 확장자 추출 (없으면 빈 문자열)
            String originalFilename = multipartFile.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";

            // 고유한 파일 이름 생성 (디렉토리/UUID.확장자 형식)
            String fileName = dirName + "/" + UUID.randomUUID() + ext;

            // S3에 파일 업로드
            s3Template.upload(bucketName, fileName, multipartFile.getInputStream());

            // 업로드된 파일의 S3 URL 반환
            return "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }
}
