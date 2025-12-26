package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThumbnailQueuePublisher {

    @Value("${aws.sqs.thumbnailQueueUrl}")
    private String queueUrl;

    @Value("${aws.region}")
    private String region;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publish(Long fileId, String fileUrl, String fileName) {
        try (SqsClient sqs = SqsClient.builder().region(Region.of(region)).build()) {
            String body = objectMapper.writeValueAsString(
                    Map.of(
                            "fileId", fileId,
                            "fileUrl", fileUrl,
                            "fileName", fileName
                    )
            );

            sqs.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(body)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("SQS send failed: " + e.getMessage(), e);
        }
    }
}
