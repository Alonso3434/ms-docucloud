package cl.duoc.ejemplo.ms.docucloud.dto;

import cl.duoc.ejemplo.ms.docucloud.model.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoRequestDto {

    private String clienteId;
    private TipoDocumento tipoDocumento;
}
