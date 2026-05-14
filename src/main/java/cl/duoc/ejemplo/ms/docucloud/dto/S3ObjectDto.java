package cl.duoc.ejemplo.ms.docucloud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class S3ObjectDto {

    private String key;
    private Long size;
    private String lastModified;
}
