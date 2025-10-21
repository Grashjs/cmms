package com.quezon.cmms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${S3_ENDPOINT:}")
    private String s3Endpoint;

    @Value("${S3_REGION:us-ashburn-1}")
    private String s3Region;

    @Value("${S3_ACCESS_KEY_ID:}")
    private String s3AccessKey;

    @Value("${S3_SECRET_ACCESS_KEY:}")
    private String s3SecretKey;

    @Value("${S3_FORCE_PATH_STYLE:false}")
    private boolean forcePathStyle;

    @Bean
    public S3Client s3Client() {
        S3Client.Builder builder = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(s3Region));

        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint));
        }

        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(forcePathStyle)
                .build();

        builder.serviceConfiguration(s3Configuration);

        if (s3AccessKey != null && !s3AccessKey.isBlank()
                && s3SecretKey != null && !s3SecretKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s3AccessKey, s3SecretKey)
                    )
            );
        }

        return builder.build();
    }
}
