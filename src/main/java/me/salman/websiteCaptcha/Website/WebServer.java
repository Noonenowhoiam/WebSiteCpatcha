package me.salman.websiteCaptcha.Website;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.Loader.CustomFileLoader;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import me.salman.websiteCaptcha.Manager.WebsiteFileManager;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class WebServer {
    private final HttpServer server;
    private final VerificationManager verificationManager;
    private final WebsiteFileManager websiteFileManager;
    private final CustomFileLoader customFileLoader;

    public WebServer(VerificationManager verificationManager) throws IllegalArgumentException, IOException {
        if (verificationManager == null) {
            throw new IllegalArgumentException("VerificationManager cannot be null!");
        }

        this.verificationManager = verificationManager;
        this.websiteFileManager = new WebsiteFileManager(Main.getInstance());
        this.customFileLoader = new CustomFileLoader(Main.getInstance());

        int port = Main.getInstance().getConfig().getInt("Web.port", 8080);
        String host = Main.getInstance().getConfig().getString("Web.host", "localhost");
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/captcha", new CaptchaHandler());
        server.createContext("/verify", new VerifyHandler(verificationManager));

        server.createContext("/styles.css", websiteFileManager.getStaticFileHandler("styles.css"));
        server.createContext("/animations.css", websiteFileManager.getStaticFileHandler("animations.css"));
        server.createContext("/script.js", websiteFileManager.getStaticFileHandler("script.js"));

        server.createContext("/pages/", new CustomFileHandler());

        Main.getInstance().getLogger().info("WebServer initialized on " + host + ":" + port);

        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void startServer() {
        server.start();
    }

    public void stopServer() {
        server.stop(0);
    }

    /**
     * Expose the HttpServer instance
     *
     * @return the HttpServer instance
     */
    public HttpServer getServer() {
        return server;
    }

    private class CustomFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String fileName = path.startsWith("/pages/") ? path.substring("/pages/".length()) : path;

            if (fileName.isEmpty() || fileName.startsWith("captcha") || fileName.startsWith("verify")) {
                sendResponse(exchange, "Not Found", "text/plain; charset=UTF-8", 404);
                return;
            }

            if (fileName.isEmpty()) {
                fileName = "index.html";
            }

            String content = tryLoadFile(exchange, fileName);
            if (content == null && !fileName.endsWith(".html")) {
                content = tryLoadFile(exchange, fileName + ".html");
            }

            if (content == null) {
                sendResponse(exchange, "Custom file not found: " + fileName, "text/plain; charset=UTF-8", 404);
                return;
            }

            String contentType = fileName.endsWith(".html") ? "text/html; charset=UTF-8" :
                    fileName.endsWith(".css") ? "text/css; charset=UTF-8" :
                            fileName.endsWith(".js") ? "application/javascript; charset=UTF-8" :
                                    "application/octet-stream";
            sendResponse(exchange, content, contentType);
        }

        private String tryLoadFile(HttpExchange exchange, String fileName) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("cacheBust", String.valueOf(System.currentTimeMillis()));
            String uuid = exchange.getRequestURI().getQuery() != null && exchange.getRequestURI().getQuery().contains("uuid=")
                    ? exchange.getRequestURI().getQuery().split("uuid=")[1].split("&")[0]
                    : verificationManager.getPlugin().getServer().getOnlinePlayers().stream()
                    .findFirst().map(Player::getUniqueId).map(UUID::toString).orElse("default-uuid");
            placeholders.put("uuid", uuid);
            return customFileLoader.loadCustomFile(fileName, placeholders);
        }

        private void sendResponse(HttpExchange exchange, String content, String contentType) throws IOException {
            sendResponse(exchange, content, contentType, 200);
        }

        private void sendResponse(HttpExchange exchange, String content, String contentType, int statusCode) throws IOException {
            byte[] responseBytes = content.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, private");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}