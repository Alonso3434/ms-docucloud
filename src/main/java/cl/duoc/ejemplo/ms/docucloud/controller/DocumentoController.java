package cl.duoc.ejemplo.ms.docucloud.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.docucloud.dto.DocumentoDto;
import cl.duoc.ejemplo.ms.docucloud.dto.DocumentoRequestDto;
import cl.duoc.ejemplo.ms.docucloud.model.TipoDocumento;
import cl.duoc.ejemplo.ms.docucloud.service.DocumentoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    /**
     * POST /documentos
     * Sube un documento al sistema. El archivo pasa primero por EFS (temporal)
     * y luego se almacena definitivamente en S3.
     *
     * Parámetros (multipart/form-data):
     *   clienteId      – identificador del cliente
     *   tipoDocumento  – BOLETA | FACTURA | ORDEN_COMPRA | COMPROBANTE_PAGO
     *   archivo        – archivo a subir
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoDto> crearDocumento(
            @RequestParam String clienteId,
            @RequestParam TipoDocumento tipoDocumento,
            @RequestParam MultipartFile archivo) {

        DocumentoDto resultado = documentoService.crearDocumento(clienteId, tipoDocumento, archivo);
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /documentos
     * Lista todos los documentos registrados.
     */
    @GetMapping
    public ResponseEntity<List<DocumentoDto>> listarDocumentos() {
        return ResponseEntity.ok(documentoService.listarDocumentos());
    }

    /**
     * GET /documentos/{id}
     * Retorna los metadatos de un documento por su id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDto> obtenerDocumento(@PathVariable String id) {
        return ResponseEntity.ok(documentoService.obtenerDocumento(id));
    }

    /**
     * GET /documentos/cliente/{clienteId}
     * Lista todos los documentos de un cliente.
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<DocumentoDto>> listarPorCliente(@PathVariable String clienteId) {
        return ResponseEntity.ok(documentoService.listarPorCliente(clienteId));
    }

    /**
     * PUT /documentos/{id}
     * Actualiza los metadatos de un documento (clienteId y/o tipoDocumento).
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDto> actualizarDocumento(
            @PathVariable String id,
            @RequestBody DocumentoRequestDto requestDto) {

        return ResponseEntity.ok(documentoService.actualizarDocumento(id, requestDto));
    }

    /**
     * DELETE /documentos/{id}
     * Elimina un documento del sistema y lo borra de S3.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable String id) {
        documentoService.eliminarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /documentos/{id}/descargar
     * Descarga el contenido binario del documento desde S3.
     */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable String id) {
        DocumentoDto meta = documentoService.obtenerDocumento(id);
        byte[] contenido = documentoService.descargarDocumento(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getNombreArchivo() + "\"")
                .contentType(meta.getContentType() != null
                        ? MediaType.parseMediaType(meta.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(contenido);
    }
}
