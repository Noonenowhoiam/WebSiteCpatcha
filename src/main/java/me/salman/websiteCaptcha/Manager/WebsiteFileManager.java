package me.salman.websiteCaptcha.Manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.Loader.CustomFileLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WebsiteFileManager {
    private final Main plugin;
    private final Map<String, String> defaultTemplates;
    private final CustomFileLoader customFileLoader;
    private boolean setupSuccessful;

    public WebsiteFileManager(Main plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null!");
        }
        this.plugin = plugin;
        this.defaultTemplates = new HashMap<>();
        this.customFileLoader = new CustomFileLoader(plugin);
        this.setupSuccessful = false;
        initializeDefaultTemplates();
        plugin.getLogger().info("WebsiteFileManager initialized.");
    }

    private void initializeDefaultTemplates() {
        defaultTemplates.put("captcha.html", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Yourself</title>
                <link rel="stylesheet" href="/styles.css?cb={cacheBust}">
                <link rel="stylesheet" href="/animations.css?cb={cacheBust}">
                <script src="/script.js?cb={cacheBust}" defer></script>
                <script src="https://www.google.com/recaptcha/api.js" async defer></script>
            </head>
            <body>
                <div class="minecraft-bg"></div>
                <div class="particles"></div>
                <div class="container">
                    <div class="dialog-box">
                        <div class="dialog-header">
                            <h1 class="minecraft-title">Verify Yourself</h1>
                        </div>
                        <div class="dialog-content">
                            <div class="dialog-message">
                                <p>Please complete the CAPTCHA below to proceed.</p>
                            </div>
                            <form id="verification-form" action="/verify" method="POST">
                                <input type="hidden" name="uuid" value="{uuid}">
                                <div class="captcha-container">
                                    <div class="g-recaptcha" data-sitekey="{siteKey}"></div>
                                    <button type="submit" class="minecraft-button">
                                        <span class="button-text">Verify</span>
                                    </button>
                                </div>
                            </form>
                            <div class="minecraft-block"></div>
                        </div>
                    </div>
                </div>
                <footer>WebsiteCaptcha Plugin</footer>
            </body>
            </html>
            """);
        defaultTemplates.put("success.html", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Captcha Verified!</title>
                <link rel="stylesheet" href="/styles.css?cb={cacheBust}">
                <link rel="stylesheet" href="/animations.css?cb={cacheBust}">
                <script src="/script.js?cb={cacheBust}" defer></script>
            </head>
            <body>
                <div class="minecraft-bg"></div>
                <div class="particles"></div>
                <div class="container">
                    <div class="dialog-box" style="animation: successPulse 1s infinite">
                        <div class="dialog-header">
                            <h1 class="minecraft-title">Captcha Verified!</h1>
                        </div>
                        <div class="dialog-content">
                            <div class="dialog-message">
                                <p>You can now return to the Minecraft server.</p>
                            </div>
                            <div class="success-checkmark">
                                <svg viewBox="0 0 80 80">
                                    <path fill="none" stroke="#62A855" stroke-width="10" stroke-linecap="round" stroke-linejoin="round" d="M20 40 L35 55 L60 30"></path>
                                </svg>
                            </div>
                            <a href="minecraft://{uuid}" class="minecraft-button">
                                <span class="button-text">Return to Server</span>
                            </a>
                            <div class="minecraft-block"></div>
                        </div>
                    </div>
                </div>
                <footer>WebsiteCaptcha Plugin</footer>
            </body>
            </html>
            """);
        defaultTemplates.put("failure.html", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>CAPTCHA Verification Failed</title>
                <link rel="stylesheet" href="/styles.css?cb={cacheBust}">
                <link rel="stylesheet" href="/animations.css?cb={cacheBust}">
                <script src="/script.js?cb={cacheBust}" defer></script>
            </head>
            <body>
                <div class="minecraft-bg"></div>
                <div class="particles"></div>
                <div class="container">
                    <div class="dialog-box">
                        <div class="dialog-header">
                            <h1 class="minecraft-title">CAPTCHA Verification Failed</h1>
                        </div>
                        <div class="dialog-content">
                            <div class="dialog-message">
                                <p>Please try again.</p>
                            </div>
                            <a href="/captcha?uuid={uuid}" class="minecraft-button">
                                <span class="button-text">Retry</span>
                            </a>
                            <div class="minecraft-block"></div>
                        </div>
                    </div>
                </div>
                <footer>WebsiteCaptcha Plugin</footer>
            </body>
            </html>
            """);
        defaultTemplates.put("invalid.html", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Invalid Request</title>
                <link rel="stylesheet" href="/styles.css?cb={cacheBust}">
                <link rel="stylesheet" href="/animations.css?cb={cacheBust}">
                <script src="/script.js?cb={cacheBust}" defer></script>
            </head>
            <body>
                <div class="minecraft-bg"></div>
                <div class="particles"></div>
                <div class="container">
                    <div class="dialog-box">
                        <div class="dialog-header">
                            <h1 class="minecraft-title">Invalid Request</h1>
                        </div>
                        <div class="dialog-content">
                            <div class="dialog-message">
                                <p>{message}</p>
                            </div>
                            <div class="minecraft-block"></div>
                        </div>
                    </div>
                </div>
                <footer>WebsiteCaptcha Plugin</footer>
            </body>
            </html>
            """);
        defaultTemplates.put("styles.css", """
            /* Minecraft Font */
            @font-face {
                font-family: 'Minecraft';
                src: url('https://fonts.cdnfonts.com/css/minecraft-4') format('woff2');
                font-weight: normal;
                font-style: normal;
            }

            /* Reset and Base Styles */
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: 'Minecraft', 'Arial', sans-serif;
                color: #FFFFFF;
                background-color: #1D1F21;
                height: 100vh;
                overflow: hidden;
                position: relative;
                animation: pageTransition 0.5s ease-out;
            }

            /* Minecraft Background */
            .minecraft-bg {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background-image: url('https://images.pexels.com/photos/1169754/pexels-photo-1169754.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1');
                background-size: 64px 64px;
                background-position: center;
                opacity: 0.3;
                z-index: -2;
            }

            .minecraft-bg::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: linear-gradient(
                    45deg,
                    rgba(0, 0, 0, 0.9) 0%,
                    rgba(20, 20, 20, 0.8) 50%,
                    rgba(0, 0, 0, 0.9) 100%
                ),
                url('https://images.pexels.com/photos/220182/pexels-photo-220182.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1');
                background-size: cover;
                background-blend-mode: multiply;
                z-index: -1;
            }

            /* Main Container */
            .container {
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
                padding: 16px;
                z-index: 1;
            }

            /* Dialog Box Styling */
            .dialog-box {
                background-color: rgba(50, 50, 59, 0.95);
                border: 4px solid #1B1B1B;
                border-radius: 0;
                box-shadow: 0 0 24px rgba(0, 0, 0, 0.8), 
                            inset 0 0 8px rgba(255, 255, 255, 0.1),
                            0 0 0 4px rgba(0, 0, 0, 0.3);
                width: 100%;
                max-width: 500px;
                overflow: hidden;
                transform: translateY(0);
                opacity: 1;
                animation: dialogAppear 0.5s ease-out;
                image-rendering: pixelated;
            }

            .dialog-header {
                background-color: #373737;
                padding: 16px;
                border-bottom: 4px solid #1B1B1B;
                text-align: center;
                position: relative;
            }

            .dialog-header::after {
                content: '';
                position: absolute;
                bottom: -4px;
                left: 0;
                width: 100%;
                height: 4px;
                background: repeating-linear-gradient(
                    to right,
                    #1B1B1B 0,
                    #1B1B1B 8px,
                    #2B2B2B 8px,
                    #2B2B2B 16px
                );
            }

            .minecraft-title {
                font-size: 24px;
                color: #FFFF55;
                text-shadow: 2px 2px 0 #3F3F00, -2px -2px 0 #000;
                letter-spacing: 1px;
            }

            .dialog-content {
                padding: 24px;
                background: repeating-linear-gradient(
                    45deg,
                    rgba(0, 0, 0, 0.2) 0,
                    rgba(0, 0, 0, 0.2) 2px,
                    transparent 2px,
                    transparent 4px
                );
            }

            .dialog-message {
                margin-bottom: 24px;
                color: #E0E0E0;
                font-size: 16px;
                line-height: 1.5;
                text-shadow: 1px 1px 0 #000;
            }

            .dialog-message p {
                margin-bottom: 16px;
            }

            /* CAPTCHA Container */
            .captcha-container {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 16px;
            }

            /* Form Styling */
            #verification-form {
                display: flex;
                flex-direction: column;
                align-items: center;
                width: 100%;
                gap: 16px;
            }

            /* Minecraft Button */
            .minecraft-button {
                background-color: #5B5B5B;
                border: none;
                border-bottom: 4px solid #373737;
                padding: 0;
                height: 40px;
                min-width: 160px;
                position: relative;
                cursor: pointer;
                transition: all 0.2s;
                outline: none;
                margin-top: 16px;
                image-rendering: pixelated;
            }

            .minecraft-button::before {
                content: '';
                position: absolute;
                left: 0;
                top: 0;
                right: 0;
                bottom: 0;
                box-shadow: inset 0 4px 0 rgba(255, 255, 255, 0.4), 
                            inset 0 -4px 0 rgba(0, 0, 0, 0.4),
                            inset 4px 0 0 rgba(255, 255, 255, 0.2),
                            inset -4px 0 0 rgba(0, 0, 0, 0.2);
                z-index: 1;
            }

            .button-text {
                position: relative;
                z-index: 2;
                display: block;
                padding: 0 16px;
                font-family: 'Minecraft', sans-serif;
                font-size: 16px;
                color: white;
                text-shadow: 2px 2px 0 #3F3F3F;
                line-height: 40px;
            }

            .minecraft-button:hover {
                animation: buttonHover 0.3s forwards;
            }

            .minecraft-button:active {
                transform: translateY(2px) scale(0.98);
                border-bottom-width: 2px;
                margin-bottom: 2px;
            }

            /* Footer */
            footer {
                position: fixed;
                bottom: 0;
                width: 100%;
                text-align: center;
                padding: 16px;
                color: #AAAAAA;
                font-size: 12px;
                z-index: 10;
                text-shadow: 1px 1px 0 #000;
            }

            /* Responsive styles */
            @media (max-width: 768px) {
                .dialog-box {
                    max-width: 100%;
                    margin: 0 16px;
                }
                
                .minecraft-title {
                    font-size: 20px;
                }
                
                .dialog-message {
                    font-size: 14px;
                }
            }

            /* reCAPTCHA custom styling */
            .g-recaptcha {
                margin: 0 auto;
                transform: scale(0.95);
                transform-origin: center;
                border: 2px solid #373737;
                background: rgba(0, 0, 0, 0.3);
                padding: 4px;
                box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.5);
            }

            @media (max-width: 320px) {
                .g-recaptcha {
                    transform: scale(0.85);
                }
            }
            """);
        defaultTemplates.put("animations.css", """
            /* Dialog Appearance Animation */
            @keyframes dialogAppear {
                0% {
                    transform: translateY(20px);
                    opacity: 0;
                }
                100% {
                    transform: translateY(0);
                    opacity: 1;
                }
            }

            /* Button Hover Animation */
            @keyframes buttonHover {
                0% {
                    background-color: #5B5B5B;
                }
                100% {
                    background-color: #62A855;
                }
            }

            /* Success Page Animation */
            @keyframes successPulse {
                0% {
                    transform: scale(1);
                }
                50% {
                    transform: scale(1.05);
                }
                100% {
                    transform: scale(1);
                }
            }

            /* Particle Animation */
            .particles {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                z-index: -1;
            }

            .particles::before {
                content: '';
                position: absolute;
                top: -10px;
                left: 0;
                width: 100%;
                height: 10px;
                background-image: 
                    radial-gradient(circle, rgba(255, 255, 255, 0.4) 20%, transparent 20%),
                    radial-gradient(circle, rgba(255, 255, 255, 0.4) 20%, transparent 20%);
                background-size: 10px 10px;
                background-position: 0 0, 5px 5px;
                animation: particleFall 15s linear infinite;
            }

            @keyframes particleFall {
                0% {
                    transform: translateY(0);
                }
                100% {
                    transform: translateY(100vh);
                }
            }

            /* Page Transition Animation */
            @keyframes pageTransition {
                0% {
                    opacity: 0;
                }
                100% {
                    opacity: 1;
                }
            }

            /* Success Animation */
            .success-checkmark {
                width: 80px;
                height: 80px;
                margin: 0 auto;
                position: relative;
                animation: successCheckmark 0.5s cubic-bezier(0.65, 0, 0.45, 1) forwards;
            }

            @keyframes successCheckmark {
                0% {
                    transform: scale(0);
                }
                100% {
                    transform: scale(1);
                }
            }

            /* Minecraft Block Animation */
            .minecraft-block {
                width: 50px;
                height: 50px;
                background-color: #8B5A2B;
                box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.2), inset 0 0 0 4px rgba(0, 0, 0, 0.2);
                position: relative;
                animation: blockFloat 3s ease-in-out infinite alternate;
            }

            @keyframes blockFloat {
                0% {
                    transform: translateY(0) rotate(0deg);
                }
                100% {
                    transform: translateY(-10px) rotate(5deg);
                }
            }

            /* Loading Animation */
            .loading-bar {
                width: 100%;
                height: 20px;
                background-color: #373737;
                position: relative;
                overflow: hidden;
                border: 2px solid #1B1B1B;
                margin: 16px 0;
            }

            .loading-bar::after {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                height: 100%;
                width: 30%;
                background-color: #62A855;
                animation: loadingProgress 2s infinite linear;
            }

            @keyframes loadingProgress {
                0% {
                    left: -30%;
                }
                100% {
                    left: 100%;
                }
            }
            """);
        defaultTemplates.put("script.js", """
            document.addEventListener('DOMContentLoaded', function() {
                // Add form submission handler
                const form = document.getElementById('verification-form');
                if (form) {
                    form.addEventListener('submit', function(event) {
                        const recaptchaResponse = grecaptcha.getResponse();
                        if (!recaptchaResponse) {
                            event.preventDefault();
                            alert('Please complete the CAPTCHA verification');
                        }
                    });
                }

                // Add particle effect
                createParticles();
            });

            function createParticles() {
                const particlesContainer = document.querySelector('.particles');
                if (!particlesContainer) return;
                
                const particleCount = 15;
                for (let i = 0; i < particleCount; i++) {
                    const particle = document.createElement('div');
                    particle.classList.add('particle');
                    
                    // Random styling
                    particle.style.position = 'absolute';
                    particle.style.width = Math.random() * 5 + 2 + 'px';
                    particle.style.height = particle.style.width;
                    particle.style.backgroundColor = 'rgba(255, 255, 255, ' + (Math.random() * 0.3 + 0.1) + ')';
                    particle.style.borderRadius = '50%';
                    
                    // Random position
                    particle.style.left = Math.random() * 100 + 'vw';
                    particle.style.top = Math.random() * 100 + 'vh';
                    
                    // Animation
                    particle.style.animation = 'floatParticle ' + (Math.random() * 15 + 10) + 's linear infinite';
                    particle.style.animationDelay = Math.random() * 5 + 's';
                    
                    particlesContainer.appendChild(particle);
                }
            }

            // Add floating animation for particles
            const styleSheet = document.createElement('style');
            styleSheet.textContent = `
                @keyframes floatParticle {
                    0% {
                        transform: translate(0, 0);
                        opacity: 0;
                    }
                    10% {
                        opacity: 1;
                    }
                    90% {
                        opacity: 1;
                    }
                    100% {
                        transform: translate(${Math.random() * 200 - 100}px, -100vh);
                        opacity: 0;
                    }
                }
                
                .particle {
                    position: absolute;
                    pointer-events: none;
                }
            `;
            document.head.appendChild(styleSheet);
            """);
    }

    public boolean setupWebsiteFolder() {
        setupSuccessful = true;
        Path websiteDir = Paths.get(plugin.getDataFolder().getPath(), "website");

        try {
            if (!Files.exists(websiteDir)) {
                Files.createDirectory(websiteDir);
                plugin.getLogger().info("Created website folder at: " + websiteDir);
            }

            for (String fileName : defaultTemplates.keySet()) {
                Path filePath = websiteDir.resolve(fileName);
                if (!Files.exists(filePath)) {
                    plugin.getLogger().warning(fileName + " is missing, generating default file.");
                    generateDefaultFile(filePath, fileName);
                }
                try {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    if (!customFileLoader.isValidFileContent(fileName, content)) {
                        throw new IOException("Malformed content detected in " + fileName);
                    }
                    plugin.getLogger().info("Loaded " + fileName + " file.");
                } catch (IOException e) {
                    plugin.getLogger().severe("Error reading " + fileName + ": " + e.getMessage());
                    plugin.getLogger().severe("Error coming from " + fileName + ". Please fix the problem to run the plugin.");
                    e.printStackTrace();
                    setupSuccessful = false;
                    return false;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to set up website folder: " + e.getMessage());
            e.printStackTrace();
            setupSuccessful = false;
            return false;
        }

        plugin.getLogger().info("All website files loaded successfully.");
        return true;
    }

    private void generateDefaultFile(Path filePath, String fileName) {
        try {
            try (InputStream resourceStream = plugin.getResource("website/" + fileName)) {
                if (resourceStream != null) {
                    Files.copy(resourceStream, filePath);
                    plugin.getLogger().info("Copied default " + fileName + " from resources.");
                    return;
                }
            }
            Files.writeString(filePath, defaultTemplates.get(fileName), StandardCharsets.UTF_8);
            plugin.getLogger().info("Generated default " + fileName + " from embedded template.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to generate default " + fileName + ": " + e.getMessage());
            e.printStackTrace();
            setupSuccessful = false;
        }
    }

    public String loadWebsiteFile(String fileName, Map<String, String> placeholders) {
        Path filePath = Paths.get(plugin.getDataFolder().getPath(), "website", fileName);
        try {
            if (!Files.exists(filePath)) {
                plugin.getLogger().warning(fileName + " is missing, generating default file.");
                generateDefaultFile(filePath, fileName);
                if (!Files.exists(filePath)) {
                    plugin.getLogger().severe("Failed to regenerate " + fileName + ".");
                    return "Error: Could not regenerate " + fileName + ".";
                }
            }
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (!customFileLoader.isValidFileContent(fileName, content)) {
                throw new IOException("Malformed content detected in " + fileName);
            }
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                content = content.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            // plugin.getLogger().info("Successfully loaded " + fileName + " for serving.");
            return content;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load " + fileName + ": " + e.getMessage());
            plugin.getLogger().severe("Error coming from " + fileName + ". Please fix the problem to run the plugin.");
            e.printStackTrace();
            return "Error loading file: " + e.getMessage();
        }
    }

    public HttpHandler getStaticFileHandler(String fileName) {
        return new StaticFileHandler(fileName);
    }

    private class StaticFileHandler implements HttpHandler {
        private final String fileName;

        public StaticFileHandler(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Path filePath = Paths.get(plugin.getDataFolder().getPath(), "website", fileName);
            if (!Files.exists(filePath)) {
                plugin.getLogger().warning(fileName + " is missing, generating default file for HTTP request.");
                generateDefaultFile(filePath, fileName);
                if (!Files.exists(filePath)) {
                    sendErrorResponse(exchange, 404, "File not found: " + fileName);
                    return;
                }
            }

            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                String contentType = fileName.endsWith(".css") ? "text/css; charset=UTF-8" :
                        fileName.endsWith(".js") ? "application/javascript; charset=UTF-8" :
                                "text/html; charset=UTF-8";
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, private");
                exchange.getResponseHeaders().set("Pragma", "no-cache");
                exchange.getResponseHeaders().set("Expires", "0");
                exchange.sendResponseHeaders(200, fileBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileBytes);
                }
                // plugin.getLogger().info("Served " + fileName + " via HTTP.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to serve " + fileName + ": " + e.getMessage());
                plugin.getLogger().severe("Error coming from " + fileName + ". Please fix the problem to run the plugin.");
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Error serving file: " + fileName);
            }
        }

        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    public boolean isSetupSuccessful() {
        return setupSuccessful;
    }
}