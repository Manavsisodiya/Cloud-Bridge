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

    // THIS IS THE MAIN METHOD RENDER IS ASKING FOR
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // This endpoint matches your frontend's fetch call
        server.createContext("/2015-03-31/functions/function/invocations", (exchange) -> {
            // Handle CORS Preflight
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // Parse the JSON body from the frontend
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> event = new Gson().fromJson(body, Map.class);

                // Trigger the core bridge logic
                Handler handler = new Handler();
                handler.handleRequest(event, null);

                // Success Response
                sendCorsHeaders(exchange);
                String response = "{\"status\":\"success\", \"message\":\"Bridge Initialized\"}";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        System.out.println("Cloud Bridge Backend Started on Port: " + port);
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
        System.out.println("BRIDGE_LOG: Sync request received.");

        String fileUrl = (String) event.get("fileUrl");
        String userAccessToken = (String) event.get("accessToken");

        // Background thread to handle the heavy streaming
        new Thread(() -> {
            try {
                System.out.println("JAVA_STREAM: Initializing stream to Google Drive...");
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
                metadata.setName("BRIDGE_FILE_" + System.currentTimeMillis());
                
                // Add Folder ID if provided in Render Env
                String folderId = System.getenv("GOOGLE_DRIVE_FOLDER_ID");
                if (folderId != null && !folderId.isEmpty()) {
                    metadata.setParents(java.util.Collections.singletonList(folderId));
                }

                InputStreamContent content = new InputStreamContent("application/octet-stream", in);
                Drive.Files.Create insert = driveService.files().create(metadata, content);
                
                // Enable Resumable Mode
                insert.getMediaHttpUploader().setDirectUploadEnabled(false);
                insert.getMediaHttpUploader().setChunkSize(8 * 1024 * 1024); 

                insert.getMediaHttpUploader().setProgressListener(uploader -> {
                    System.out.println("Upload Progress: " + (int)(uploader.getProgress() * 100) + "%");
                });
                
                insert.execute();
                System.out.println("JAVA_SUCCESS: File successfully bridged to Drive.");

            } catch (Exception e) {
                System.err.println("JAVA_ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        return new HashMap<>();
    }
}
