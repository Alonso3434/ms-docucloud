package cl.duoc.ejemplo.ms.docucloud.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EfsService {

    @Value("${efs.path}")
    private String efsPath;

    /**
     * Guarda los bytes del archivo de forma temporal en el path montado de EFS.
     * Recibe byte[] para evitar problemas con transferTo() que mueve el temp file de Tomcat.
     */
    public File saveToEfs(String filename, byte[] contenido) throws IOException {

        File dest = new File(efsPath, filename);
        File parentDir = dest.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        Files.write(Path.of(dest.getAbsolutePath()), contenido);
        return dest;
    }

    /**
     * Elimina un archivo temporal del EFS si todavía existe.
     */
    public void deleteFromEfs(String filename) {
        File file = new File(efsPath, filename);
        if (file.exists()) {
            file.delete();
        }
    }
}
