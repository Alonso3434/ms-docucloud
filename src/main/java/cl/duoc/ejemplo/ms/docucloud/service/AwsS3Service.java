package cl.duoc.ejemplo.ms.docucloud.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cl.duoc.ejemplo.ms.docucloud.dto.S3ObjectDto;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Client s3Client;

    /**
     * Lista objetos del bucket. Si se provee prefix filtra por prefijo
     * (ej: "documentos/cliente-001/" para listar solo de ese cliente).
     */
    public List<S3ObjectDto> listObjects(String bucket, String prefix) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucket);
        if (StringUtils.hasText(prefix)) {
            builder.prefix(prefix);
        }
        return s3Client.listObjectsV2(builder.build()).contents().stream()
                .map(obj -> new S3ObjectDto(
                        obj.key(),
                        obj.size(),
                        obj.lastModified() != null ? obj.lastModified().toString() : null))
                .collect(Collectors.toList());
    }

    /**
     * Retorna los metadatos del objeto (content-type, content-length, user metadata).
     * Los metadatos de usuario se almacenan bajo la clave en minúsculas, sin el
     * prefijo "x-amz-meta-" que agrega AWS internamente.
     */
    public HeadObjectResponse headObject(String bucket, String key) {
        return s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public byte[] downloadAsBytes(String bucket, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();
        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return responseBytes.asByteArray();
    }

    /**
     * Sube un archivo a S3 incluyendo metadatos de usuario (ej: tipo-documento).
     */
    public void upload(String bucket, String key, byte[] contenido, String contentType,
            Map<String, String> metadata) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) contenido.length)
                    .metadata(metadata != null ? metadata : Map.of())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(contenido));
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a S3: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza un objeto: copia con nuevos metadatos y, si la clave cambió,
     * elimina la fuente. Usa MetadataDirective.REPLACE para sobreescribir
     * los metadatos existentes con los nuevos.
     */
    public void updateObject(String bucket, String sourceKey, String destKey,
            String contentType, Map<String, String> metadata) {
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucket).sourceKey(sourceKey)
                .destinationBucket(bucket).destinationKey(destKey)
                .contentType(contentType)
                .metadata(metadata)
                .metadataDirective(MetadataDirective.REPLACE)
                .build();
        s3Client.copyObject(copyRequest);
        if (!sourceKey.equals(destKey)) {
            deleteObject(bucket, sourceKey);
        }
    }

    public void deleteObject(String bucket, String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        s3Client.deleteObject(deleteRequest);
    }
}
