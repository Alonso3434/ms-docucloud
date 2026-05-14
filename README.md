# ms-docucloud – DocuCloud: Gestión de Documentos Comerciales en la Nube

Microservicio Spring Boot para cargar, consultar y gestionar documentos comerciales utilizando **Amazon EFS** como almacenamiento temporal y **Amazon S3** como almacenamiento persistente.

## Tecnologías

| Herramienta | Uso |
|---|---|
| Spring Boot 3.3.12 | Framework backend |
| Java 21 | Lenguaje |
| Spring Cloud AWS 3.3.1 | Integración con S3 |
| Lombok | Reducción de boilerplate |
| Docker | Contenerización |
| Docker Hub | Registro de imágenes |
| GitHub Actions | Pipeline CI/CD |
| Amazon EC2 | Despliegue en la nube |
| Amazon S3 | Almacenamiento persistente de documentos |
| Amazon EFS | Almacenamiento temporal durante procesamiento |

---

## Endpoints REST

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/documentos` | Sube un documento (multipart) |
| `GET` | `/documentos` | Lista todos los documentos |
| `GET` | `/documentos/{id}` | Obtiene metadatos de un documento |
| `GET` | `/documentos/cliente/{clienteId}` | Lista documentos de un cliente |
| `PUT` | `/documentos/{id}` | Actualiza metadatos del documento |
| `DELETE` | `/documentos/{id}` | Elimina el documento (S3 + registro) |
| `GET` | `/documentos/{id}/descargar` | Descarga el archivo desde S3 |

### Ejemplo POST `/documentos` (Postman)

```
POST http://<EC2_HOST>:8080/documentos
Content-Type: multipart/form-data

clienteId:     cliente-001
tipoDocumento: FACTURA
archivo:       <adjuntar archivo>
```

### Ejemplo PUT `/documentos/{id}`

```json
{
  "clienteId": "cliente-002",
  "tipoDocumento": "BOLETA"
}
```

---

## Estructura de archivos en S3

```
documentos/
  cliente-001/
    2026-05-12/
      factura-001.pdf
  cliente-002/
    2026-05-13/
      boleta-007.pdf
```

---

## Flujo CI/CD

```
push a main
     │
     ▼
GitHub Actions
     │
     ├─ mvn clean package
     ├─ docker build
     ├─ docker push → Docker Hub
     └─ SSH a EC2
          ├─ docker pull
          └─ docker run (con variables de entorno)
```

---

## Variables de entorno requeridas

| Variable | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | Clave de acceso AWS |
| `AWS_SECRET_ACCESS_KEY` | Clave secreta AWS |
| `AWS_REGION` | Región AWS (ej: `us-east-1`) |
| `AWS_S3_BUCKET` | Nombre del bucket S3 |
| `EFS_PATH` | Path de montaje EFS (default: `/app/efs`) |

> **Nunca incluir credenciales en el código fuente ni en el repositorio.**  
> Usar GitHub Secrets para el pipeline y variables de entorno en EC2.

---

## Secrets de GitHub Actions requeridos

| Secret | Descripción |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso Docker Hub |
| `EC2_HOST` | IP o DNS público de la instancia EC2 |
| `EC2_USER` | Usuario SSH (ej: `ec2-user`) |
| `EC2_SSH_KEY` | Clave privada SSH (PEM) |
| `AWS_ACCESS_KEY_ID` | Clave de acceso AWS |
| `AWS_SECRET_ACCESS_KEY` | Clave secreta AWS |
| `AWS_REGION` | Región AWS |
| `AWS_S3_BUCKET` | Nombre del bucket S3 |

---

## Ejecución local (sin AWS)

```bash
mvn clean spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -DAWS_ACCESS_KEY_ID=local \
    -DAWS_SECRET_ACCESS_KEY=local \
    -DAWS_REGION=us-east-1 \
    -DAWS_S3_BUCKET=mi-bucket \
    -DEFS_PATH=/tmp/efs"
```

---

## Build y despliegue manual con Docker

```bash
# Construir imagen
docker build -t ms-docucloud:latest .

# Ejecutar contenedor
docker run -d \
  --name ms-docucloud \
  -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID=<valor> \
  -e AWS_SECRET_ACCESS_KEY=<valor> \
  -e AWS_REGION=us-east-1 \
  -e AWS_S3_BUCKET=docucloud-bucket \
  -v /mnt/efs:/app/efs \
  ms-docucloud:latest
```
