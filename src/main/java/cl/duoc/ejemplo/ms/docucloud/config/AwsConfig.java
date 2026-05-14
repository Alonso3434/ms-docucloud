package cl.duoc.ejemplo.ms.docucloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    /**
     * Session token requerido para credenciales temporales de AWS Academy / AssumeRole.
     * Las credenciales temporales tienen Access Key Id con prefijo ASIA (no AKIA).
     */
    @Value("${spring.cloud.aws.credentials.session-token:}")
    private String sessionToken;

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String region;

    /**
     * S3Client configurado manualmente para soportar credenciales temporales con session token.
     * Spring Cloud AWS autoconfiguration no incluye el x-amz-security-token en la firma
     * cuando el session token se provee solo via properties; este bean lo resuelve.
     */
    @Bean
    @Primary
    public S3Client s3Client() {
        AwsCredentials credentials;

        if (StringUtils.hasText(sessionToken)) {
            credentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        } else {
            credentials = AwsBasicCredentials.create(accessKey, secretKey);
        }

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
