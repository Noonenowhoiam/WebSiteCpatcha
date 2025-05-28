package me.salman.websiteCaptcha.Website;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.salman.websiteCaptcha.Api.VoidWorldManager;
import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class VerifyHandler implements HttpHandler {
    private final VerificationManager verificationManager;

    public VerifyHandler(VerificationManager verificationManager) throws IllegalArgumentException {
        if (verificationManager == null) {
            throw new IllegalArgumentException("VerificationManager cannot be null!");
        }
        this.verificationManager = verificationManager;
        Main.getInstance().getLogger().info("VerifyHandler initialized.");
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendHtmlResponse(exchange, 405, loadTemplate("invalid.html", Map.of("message", "Method Not Allowed", "cacheBust", String.valueOf(System.currentTimeMillis()))));
                return;
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(requestBody);
            String uuid = params.get("uuid");
            String recaptchaResponse = params.get("g-recaptcha-response");

            if (uuid == null || recaptchaResponse == null) {
                sendHtmlResponse(exchange, 400, loadTemplate("invalid.html", Map.of("message", "UUID or CAPTCHA response is missing", "cacheBust", String.valueOf(System.currentTimeMillis()))));
                return;
            }

            UUID playerUUID;
            try {
                playerUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                sendHtmlResponse(exchange, 400, loadTemplate("invalid.html", Map.of("message", "The provided UUID is malformed", "cacheBust", String.valueOf(System.currentTimeMillis()))));
                return;
            }

            if (verifyCaptcha(recaptchaResponse)) {
                if (verificationManager == null) {
                    Main.getInstance().getLogger().severe("VerificationManager is null in VerifyHandler!");
                    sendErrorResponse(exchange, 500, "Server error: Verification manager not initialized");
                    return;
                }
                verificationManager.setPlayerVerified(playerUUID, true);

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null && player.isOnline()) {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        if (Main.getInstance().getConfig().getBoolean("options.hide_inventory")) {
                            Main.getInstance().getInventoryManager().restoreInventory(player);
                            player.sendMessage(formatMessage(Main.getInstance().getConfig().getString("messages.inventory_restored")));
                        }
                        player.sendMessage(formatMessage(Main.getInstance().getConfig().getString("messages.verification_success")));
                        VoidWorldManager.sendToNormalWorld(player);
                        Main.getInstance().getLogger().info("Player " + playerUUID + " verified and sent to normal world.");

                        Integer taskId = verificationManager.getVerificationTask(playerUUID);
                        if (taskId != null) {
                            Bukkit.getScheduler().cancelTask(taskId);
                            verificationManager.removeVerificationTask(playerUUID);
                        }
                    } else {
                        Main.getInstance().getLogger().info("Player " + playerUUID + " not online after verification.");
                    }
                }, 2L);

                sendHtmlResponse(exchange, 200, loadTemplate("success.html", Map.of("uuid", uuid, "cacheBust", String.valueOf(System.currentTimeMillis()))));
            } else {
                Main.getInstance().getLogger().info("Verification failed for UUID: " + uuid + " with error: timeout-or-duplicate");
                sendHtmlResponse(exchange, 400, loadTemplate("failure.html", Map.of("uuid", uuid, "cacheBust", String.valueOf(System.currentTimeMillis()))));
            }
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Error processing /verify request: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error processing request");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Unexpected error in /verify: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error processing request");
        } finally {
            try {
                exchange.close();
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Failed to close HttpExchange: " + e.getMessage());
            }
        }
    }

    private String loadTemplate(String templateName, Map<String, String> placeholders) {
        return Main.getInstance().getWebsiteFileManager().loadWebsiteFile(templateName, placeholders);
    }

    private boolean verifyCaptcha(String recaptchaResponse) {
        try {
            String secretKey = Main.getInstance().getConfig().getString("recaptcha.secret_key");
            if (secretKey == null || secretKey.isEmpty()) {
                Main.getInstance().getLogger().severe("reCAPTCHA secret key is not configured!");
                return false;
            }

            URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String params = "secret=" + secretKey + "&response=" + recaptchaResponse;
            byte[] postData = params.getBytes(StandardCharsets.UTF_8);

            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postData.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData);
            }

            Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            return response.contains("\"success\": true");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Failed to verify CAPTCHA: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        if (formData != null && !formData.isEmpty()) {
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    private void sendHtmlResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
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

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) {
        try {
            String errorHtml = loadTemplate("invalid.html", Map.of("message", errorMessage, "cacheBust", String.valueOf(System.currentTimeMillis())));
            sendHtmlResponse(exchange, statusCode, errorHtml);
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Failed to send error response: " + e.getMessage());
        }
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}