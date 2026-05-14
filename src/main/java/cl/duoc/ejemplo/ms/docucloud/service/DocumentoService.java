package cl.duoc.ejemplo.ms.docucloud.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.docucloud.dto.DocumentoDto;
import cl.duoc.ejemplo.ms.docucloud.dto.DocumentoRequestDto;
import cl.duoc.ejemplo.ms.docucloud.model.TipoDocumento;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final EfsService efsService;
    private final AwsS3Service awsS3Service;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final String PREFIX_BASE = "documentos/";
    private static final String META_TIPO   = "tipo-documento";

    // ──────────────────────────────────────────────────────────────────────────
    // El ID que se expone al cliente es la S3 key codificada en Base64 URL-safe.
    // Esto evita los '/' del path y hace que el ID sea opaco y seguro para URLs.
    // S3 es la ÚNICA fuente de verdad: no existe repositorio en memoria.
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Flujo:
     * 1. Lee los bytes del archivo en memoria.
     * 2. Guarda temporalmente en EFS (simula procesamiento inicial).
     * 3. Sube a S3 con metadatos (tipoDocumento).
     * 4. Elimina temporal de EFS.
     * 5. Consulta HeadObject en S3 y retorna el DTO.
     */
    public DocumentoDto crearDocumento(String clienteId, TipoDocumento tipoDocumento, MultipartFile archivo) {

        String nombreArchivo = archivo.getOriginalFilename();
        String contentType   = archivo.getContentType();
        String tempFilename  = UUID.randomUUID() + "_" + nombreArchivo;

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo: " + e.getMessage(), e);
        }

        // Paso 1 – EFS temporal
        try {
            efsService.saveToEfs(tempFilename, contenido);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar en EFS: " + e.getMessage(), e);
        }

        // Paso 2 – clave S3: documentos/{clienteId}/{fecha}/{nombreArchivo}
        LocalDate hoy   = LocalDate.now();
        String s3Key    = String.format("documentos/%s/%s/%s", clienteId, hoy, nombreArchivo);

        // Paso 3 – subir a S3 con metadatos de usuario
        Map<String, String> metadata = new HashMap<>();
        metadata.put(META_TIPO, tipoDocumento.name());
        awsS3Service.upload(bucket, s3Key, contenido, contentType, metadata);

        // Paso 4 – limpiar EFS
        efsService.deleteFromEfs(tempFilename);

        // Paso 5 – construir DTO desde S3
        HeadObjectResponse head = awsS3Service.headObject(bucket, s3Key);
        return buildDto(s3Key, head, (long) contenido.length);
    }

    /**
     * Lista todos los documentos del bucket consultando S3.
     * Por cada objeto se hace un HeadObject para obtener los metadatos de usuario.
     */
    public List<DocumentoDto> listarDocumentos() {
        return awsS3Service.listObjects(bucket, PREFIX_BASE).stream()
                .map(obj -> {
                    HeadObjectResponse head = awsS3Service.headObject(bucket, obj.getKey());
                    return buildDto(obj.getKey(), head, obj.getSize());
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los metadatos de un documento desde S3 usando su ID (S3 key en Base64).
     */
    public DocumentoDto obtenerDocumento(String id) {
        String s3Key = decodeId(id);
        HeadObjectResponse head = awsS3Service.headObject(bucket, s3Key);
        return buildDto(s3Key, head, head.contentLength());
    }

    /**
     * Lista documentos de un cliente filtrando por prefijo S3: documentos/{clienteId}/
     */
    public List<DocumentoDto> listarPorCliente(String clienteId) {
        String prefix = PREFIX_BASE + clienteId + "/";
        return awsS3Service.listObjects(bucket, prefix).stream()
                .map(obj -> {
                    HeadObjectResponse head = awsS3Service.headObject(bucket, obj.getKey());
                    return buildDto(obj.getKey(), head, obj.getSize());
                })
                .collect(Collectors.toList());
    }

    /**
     * Actualiza metadatos del documento en S3:
     * - Si cambia clienteId: mueve el objeto a una nueva clave S3.
     * - Si cambia tipoDocumento: copia el objeto con nuevos metadatos (MetadataDirective.REPLACE).
     * Retorna el DTO con el nuevo ID (puede cambiar si cambia la clave).
     */
    public DocumentoDto actualizarDocumento(String id, DocumentoRequestDto requestDto) {
        String currentKey = decodeId(id);
        HeadObjectResponse currentHead = awsS3Service.headObject(bucket, currentKey);

        // documentos/{clienteId}/{fecha}/{filename}
        String[] parts          = currentKey.split("/");
        String currentClienteId = parts[1];
        String fecha            = parts[2];
        String filename         = parts[3];

        String newClienteId = requestDto.getClienteId() != null
                ? requestDto.getClienteId() : currentClienteId;
        String newTipo = requestDto.getTipoDocumento() != null
                ? requestDto.getTipoDocumento().name()
                : currentHead.metadata().getOrDefault(META_TIPO, "");

        String newKey = String.format("documentos/%s/%s/%s", newClienteId, fecha, filename);

        Map<String, String> newMetadata = new HashMap<>(currentHead.metadata());
        newMetadata.put(META_TIPO, newTipo);

        awsS3Service.updateObject(bucket, currentKey, newKey, currentHead.contentType(), newMetadata);

        HeadObjectResponse updatedHead = awsS3Service.headObject(bucket, newKey);
        return buildDto(newKey, updatedHead, updatedHead.contentLength());
    }

    /**
     * Elimina el objeto de S3.
     */
    public void eliminarDocumento(String id) {
        String s3Key = decodeId(id);
        awsS3Service.deleteObject(bucket, s3Key);
    }

    /**
     * Descarga el contenido binario del objeto desde S3.
     */
    public byte[] descargarDocumento(String id) {
        String s3Key = decodeId(id);
        return awsS3Service.downloadAsBytes(bucket, s3Key);
    }

    // ──────────────────────────────── helpers ────────────────────────────────

    /**
     * Codifica la S3 key en Base64 URL-safe (sin padding) para usarla como ID en la API.
     * Ejemplo: "documentos/cli-001/2026-05-13/factura.pdf" → "ZG9jdW1lbnRvcy9..."
     */
    private String encodeId(String s3Key) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s3Key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodifica el ID Base64 para obtener la S3 key original.
     */
    private String decodeId(String id) {
        try {
            return new String(Base64.getUrlDecoder().decode(id), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("ID de documento inválido: " + id, e);
        }
    }

    /**
     * Construye el DocumentoDto a partir de la S3 key y la respuesta de HeadObject.
     * La S3 key codifica: documentos/{clienteId}/{fecha}/{nombreArchivo}
     */
    private DocumentoDto buildDto(String s3Key, HeadObjectResponse head, Long size) {
        String[] parts   = s3Key.split("/");
        String clienteId = parts.length > 1 ? parts[1] : "";
        String fechaStr  = parts.length > 2 ? parts[2] : "";
        String filename  = parts.length > 3 ? parts[3] : s3Key;

        LocalDate fecha = null;
        try { fecha = LocalDate.parse(fechaStr); } catch (Exception ignored) { }

        TipoDocumento tipo = null;
        String tipoStr = head.metadata().getOrDefault(META_TIPO, "");
        try { tipo = TipoDocumento.valueOf(tipoStr); } catch (Exception ignored) { }

        return DocumentoDto.builder()
                .id(encodeId(s3Key))
                .clienteId(clienteId)
                .tipoDocumento(tipo)
                .nombreArchivo(filename)
                .fechaSubida(fecha)
                .s3Key(s3Key)
                .tamanioBytes(size)
                .contentType(head.contentType())
                .estado("ALMACENADO")
                .build();
    }
}

