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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // --- RENDER STARTUP LOGIC ---
    public static void main(String[] args) throws Exception {
        // Listen on the port Render provides, or default to 8080
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // This context handles the POST request from your Vercel frontend
        server.createContext("/", (exchange) -> {
            // 1. Set CORS Headers (Crucial for Vercel to communicate with Render)
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // 2. Read the JSON body sent from Vercel
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> event = new Gson().fromJson(body, Map.class);

                // 3. Trigger the bridge logic
                Handler handler = new Handler();
                handler.handleRequest(event, null);

                // 4. Send Success Response back to Frontend
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

        System.out.println("SERVER: Cloud Bridge is live on port " + port);
        server.setExecutor(null); 
        server.start();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        // Use System.out for Render logs since Context might be null
        System.out.println("BRIDGE: Received sync request.");

        String fileUrl = (String) event.get("fileUrl");
        String userAccessToken = (String) event.get("accessToken");

        new Thread(() -> {
            try {
                System.out.println("BRIDGE_STREAM: Initializing resumable tunnel to Google Drive...");

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
                metadata.setName("CLOUD_BRIDGE_" + System.currentTimeMillis());
                
                // Optional: Set a specific folder if provided in Render Env
                String folderId = System.getenv("GOOGLE_DRIVE_FOLDER_ID");
                if (folderId != null && !folderId.isEmpty()) {
                    metadata.setParents(Collections.singletonList(folderId));
                }

                InputStreamContent content = new InputStreamContent("application/octet-stream", in);
                Drive.Files.Create insert = driveService.files().create(metadata, content);
                
                // Resumable Upload Settings
                insert.getMediaHttpUploader().setDirectUploadEnabled(false);
                insert.getMediaHttpUploader().setChunkSize(8 * 1024 * 1024); // 8MB chunks

                insert.getMediaHttpUploader().setProgressListener(uploader -> {
                    System.out.println("Sync Progress: " + (int)(uploader.getProgress() * 100) + "%");
                });
                
                File uploadedFile = insert.execute();
                System.out.println("BRIDGE_SUCCESS: File saved with ID: " + uploadedFile.getId());

            } catch (Exception e) {
                System.err.println("BRIDGE_ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        return new HashMap<>();
    }
}
