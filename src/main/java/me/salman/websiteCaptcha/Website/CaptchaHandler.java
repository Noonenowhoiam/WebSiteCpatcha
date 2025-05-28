package me.salman.websiteCaptcha.Website;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.salman.websiteCaptcha.Main;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CaptchaHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String siteKey = Main.getInstance().getConfig().getString("recaptcha.site_key");
        if (siteKey == null || siteKey.isEmpty()) {
            String errorMessage = "reCAPTCHA site key is not configured!";
            Main.getInstance().getLogger().severe(errorMessage);
            sendResponse(exchange, 500, errorMessage);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("uuid=")) {
            String errorMessage = "Missing UUID in the query!";
            Main.getInstance().getLogger().warning("Invalid /captcha request: " + query);
            sendResponse(exchange, 400, errorMessage);
            return;
        }

        String uuid = query.split("uuid=")[1];
        String cacheBust = String.valueOf(System.currentTimeMillis());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("cacheBust", cacheBust);
        placeholders.put("uuid", uuid);
        placeholders.put("siteKey", siteKey);

        String response = Main.getInstance().getWebsiteFileManager().loadWebsiteFile("captcha.html", placeholders);
        if (response.startsWith("Error")) {
            sendResponse(exchange, 500, response);
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, private");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}