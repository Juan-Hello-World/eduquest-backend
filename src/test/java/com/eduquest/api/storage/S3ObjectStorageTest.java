package com.eduquest.api.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ObjectStorageTest {

    private S3Client client;
    private S3Presigner presigner;
    private S3ObjectStorage storage;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        presigner = mock(S3Presigner.class);
        storage = new S3ObjectStorage(client, presigner, "eduquest-media");
    }

    @Test
    void constructor_requiresBucket() {
        assertThatThrownBy(() -> new S3ObjectStorage(client, presigner, " "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void putGetAndDelete_useTheConfiguredBucket() {
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[]{3}));

        storage.put("files/a/b.pdf", new byte[]{1}, "application/pdf");
        byte[] data = storage.get("files/a/b.pdf");
        storage.delete("files/a/b.pdf");

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().bucket()).isEqualTo("eduquest-media");
        assertThat(put.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(data).containsExactly(3);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(delete.capture());
        assertThat(delete.getValue().key()).isEqualTo("files/a/b.pdf");
    }

    @Test
    void temporaryUrl_signsForTenMinutesWithTheRightDisposition() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://eduquest-media.s3.amazonaws.com/files/a/b.pdf?sig=1").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = storage.temporaryUrl("files/a/b.pdf", "Cálculo.pdf", true);

        ArgumentCaptor<GetObjectPresignRequest> request = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(request.capture());
        assertThat(url).isEqualTo("https://eduquest-media.s3.amazonaws.com/files/a/b.pdf?sig=1");
        assertThat(request.getValue().signatureDuration()).isEqualTo(S3ObjectStorage.URL_LIFETIME);
        assertThat(request.getValue().getObjectRequest().responseContentDisposition()).startsWith("attachment");
        assertThat(request.getValue().getObjectRequest().bucket()).isEqualTo("eduquest-media");
    }
}
