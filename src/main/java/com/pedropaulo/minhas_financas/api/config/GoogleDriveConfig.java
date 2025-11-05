package com.pedropaulo.minhas_financas.api.config;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.List;

@Configuration
public class GoogleDriveConfig {

    @Bean
    public Drive driveClient() throws Exception {
        InputStream credStream = getClass().getResourceAsStream("/service-account.json");
        if (credStream == null) {
            throw new IllegalStateException("Credenciais não encontradas em /service-account.json");
        }

        var creds = ServiceAccountCredentials.fromStream(credStream)
                .createScoped(List.of(DriveScopes.DRIVE_FILE));

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds)
        ).setApplicationName("MinhasFinancas").build();
    }
}
