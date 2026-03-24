package com.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("JAVA: Bridge Request Received!");

        String fileUrl = (String) event.get("fileUrl");
        String userAccessToken = (String) event.get("accessToken");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");

        new Thread(() -> {
            try {
                context.getLogger().log("JAVA_BG: Using User OAuth Token for Large Sync...");

                GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(userAccessToken, null));

                Drive driveService = new Drive.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials)
                ).setApplicationName("CloudBridge").build();

                URL url = new URL(fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                InputStream in = conn.getInputStream();

                File metadata = new File();
                // Changed to octet-stream to be more generic for any file type (not just .jpg)
                metadata.setName("BRIDGE_RESUMABLE_SYNC_" + System.currentTimeMillis());
                
                InputStreamContent content = new InputStreamContent("application/octet-stream", in);
                
                Drive.Files.Create insert = driveService.files().create(metadata, content);
                
                // --- RESUMABLE UPLOAD CONFIGURATION ---
                // Disable Direct Upload to enable Resumable Mode (Standard for files > 5MB)
                insert.getMediaHttpUploader().setDirectUploadEnabled(false);
                
                // Set Chunk Size to 8MB. This protects your MacBook's RAM.
                // The backend only holds 8MB of the file at any given time.
                insert.getMediaHttpUploader().setChunkSize(8 * 1024 * 1024); 

                // Progress Listener to see the status in Docker logs
                insert.getMediaHttpUploader().setProgressListener(uploader -> {
                    try {
                        context.getLogger().log("Upload Progress: " + (uploader.getProgress() * 100) + "%");
                    } catch (Exception e) {
                        // Progress calculation error, non-fatal
                    }
                });
                
                File uploadedFile = insert.execute();
                context.getLogger().log("JAVA_SUCCESS: Big File saved! ID: " + uploadedFile.getId());

            } catch (Exception e) {
                context.getLogger().log("JAVA_FATAL_ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("headers", headers);
        response.put("body", "{\"status\": \"success\", \"message\": \"Resumable OAuth Bridge Active.\"}");

        return response;
    }
}
