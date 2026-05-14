package cl.duoc.ejemplo.ms.docucloud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Documento {

    private String id;
    private String clienteId;
    private TipoDocumento tipoDocumento;
    private String nombreArchivo;
    private LocalDate fechaSubida;
    private String s3Key;
    private Long tamanioBytes;
    private String contentType;

    /**
     * PROCESANDO: almacenado temporalmente en EFS.
     * ALMACENADO: trasladado definitivamente a S3.
     */
    private String estado;
}
