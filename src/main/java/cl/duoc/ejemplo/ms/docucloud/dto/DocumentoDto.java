package cl.duoc.ejemplo.ms.docucloud.dto;

import cl.duoc.ejemplo.ms.docucloud.model.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoDto {

    private String id;
    private String clienteId;
    private TipoDocumento tipoDocumento;
    private String nombreArchivo;
    private LocalDate fechaSubida;
    private String s3Key;
    private Long tamanioBytes;
    private String contentType;
    private String estado;
}
