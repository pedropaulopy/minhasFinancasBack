package com.pedropaulo.minhas_financas.api.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStreamReader;
import java.util.List;

@Configuration
public class GoogleDriveConfig {

	private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static final String APPLICATION_NAME = "MinhasFinancas";

	@Bean
	public Credential googleCredential() throws Exception {
		final NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
		try (var in = getClass().getResourceAsStream("/client_secret.json")) {
			if (in == null)
				throw new IllegalStateException("client_secret.json não encontrado");

			GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
			List<String> scopes = List.of(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE,
					"https://www.googleapis.com/auth/spreadsheets");

			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(http, JSON_FACTORY, secrets,
					scopes)
				.setAccessType("offline")
				.setApprovalPrompt("force")
				.build();

			LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
			return new AuthorizationCodeInstalledApp(flow, receiver).authorize("minhas-financas-user");
		}
	}

	@Bean
	public Drive driveClient(Credential credential) throws Exception {
		final NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
		return new Drive.Builder(http, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	}

	@Bean
	public Sheets sheetsClient(Credential credential) throws Exception {
		final NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
		return new Sheets.Builder(http, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	}

}
