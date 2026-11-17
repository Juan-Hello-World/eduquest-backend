package com.eduquest.api.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class S3StorageConfigTest {

    @Test
    void createsClientsForTheConfiguredRegion() {
        S3StorageConfig config = new S3StorageConfig();
        try (S3Client client = config.s3Client("us-east-1");
             S3Presigner presigner = config.s3Presigner("us-east-1")) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.US_EAST_1);
            assertThat(presigner).isNotNull();
        }
    }
}
