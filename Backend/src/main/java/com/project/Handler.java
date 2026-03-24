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
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // --- RENDER STARTUP METHOD ---
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // This matches the path in your frontend page.tsx
        server.createContext("/2015-03-31/functions/function/invocations", (exchange) -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // Read the JSON body from the frontend
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> event = new Gson().fromJson(body, Map.class);

                // Execute the existing logic
                Handler handler = new Handler();
                handler.handleRequest(event, null);

                // Send success back to frontend
                sendCorsHeaders(exchange);
                String response = "{\"status\":\"success\"}";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        System.out.println("Cloud Bridge Backend Live on Port " + port);
        server.setExecutor(null);
        server.start();
    }

    private static void sendCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        // We handle null context for Render environment
        String msgPrefix = (context != null) ? "JAVA: " : "RENDER_LOG: ";
        System.out.println(msgPrefix + "Bridge Request Received!");

        String fileUrl = (String) event.get("fileUrl");
        String userAccessToken = (String) event.get("accessToken");

        new Thread(() -> {
            try {
                System.out.println("JAVA_BG: Starting Resumable Sync...");

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
                metadata.setName("BRIDGE_SYNC_" + System.currentTimeMillis());
                
                // Set folder if env variable exists
                String folderId = System.getenv("GOOGLE_DRIVE_FOLDER_ID");
                if (folderId != null && !folderId.isEmpty()) {
                    java.util.List<String> parents = java.util.Collections.singletonList(folderId);
                    metadata.setParents(parents);
                }

                InputStreamContent content = new InputStreamContent("application/octet-stream", in);
                Drive.Files.Create insert = driveService.files().create(metadata, content);
                
                insert.getMediaHttpUploader().setDirectUploadEnabled(false);
                insert.getMediaHttpUploader().setChunkSize(8 * 1024 * 1024); 

                insert.getMediaHttpUploader().setProgressListener(uploader -> {
                    System.out.println("Upload Progress: " + (uploader.getProgress() * 100) + "%");
                });
                
                File uploadedFile = insert.execute();
                System.out.println("JAVA_SUCCESS: Saved ID: " + uploadedFile.getId());

            } catch (Exception e) {
                System.err.println("JAVA_FATAL_ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        return response;
    }
}
