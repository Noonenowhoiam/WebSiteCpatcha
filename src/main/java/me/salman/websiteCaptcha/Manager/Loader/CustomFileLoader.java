package me.salman.websiteCaptcha.Manager.Loader;

import me.salman.websiteCaptcha.Main;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CustomFileLoader {
    private final Main plugin;
    private final Path customDir;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final String[] ALLOWED_EXTENSIONS = {".html", ".css", ".js"};

    public CustomFileLoader(Main plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null!");
        }
        this.plugin = plugin;
        this.customDir = Paths.get(plugin.getDataFolder().getPath(), "website", "custom");
        initializeCustomDirectory();
    }

    private void initializeCustomDirectory() {
        try {
            if (!Files.exists(customDir)) {
                Files.createDirectories(customDir);
                plugin.getLogger().info("Created custom directory at: " + customDir);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create custom directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, String> loadCustomFiles() {
        Map<String, String> customFiles = new HashMap<>();
        try (Stream<Path> paths = Files.list(customDir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        boolean hasAllowedExtension = false;
                        for (String ext : ALLOWED_EXTENSIONS) {
                            if (fileName.toLowerCase().endsWith(ext)) {
                                hasAllowedExtension = true;
                                break;
                            }
                        }
                        if (!hasAllowedExtension) {
                            plugin.getLogger().warning("Skipping file " + fileName + " due to unsupported extension.");
                            return;
                        }

                        try {
                            String content = Files.readString(filePath, StandardCharsets.UTF_8);
                            if (isValidFileContent(fileName, content)) {
                                customFiles.put(fileName, content);
//                                plugin.getLogger().info("Loaded custom file: " + fileName);
                            } else {
                                plugin.getLogger().warning("Skipped custom file " + fileName + " due to malformed content.");
                            }
                        } catch (IOException e) {
                            plugin.getLogger().severe("Error reading custom file " + fileName + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to list custom files in " + customDir + ": " + e.getMessage());
            e.printStackTrace();
        }
        return customFiles;
    }

    public String loadCustomFile(String fileName, Map<String, String> placeholders) {
        if (fileName == null || fileName.isEmpty()) {
            plugin.getLogger().warning("Invalid file name provided for custom file loading.");
            return null;
        }

        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            plugin.getLogger().warning("Invalid file name: " + fileName + " (Directory traversal attempt detected).");
            return null;
        }

        boolean hasAllowedExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (fileName.toLowerCase().endsWith(ext)) {
                hasAllowedExtension = true;
                break;
            }
        }
        if (!hasAllowedExtension) {
            plugin.getLogger().warning("File " + fileName + " has unsupported extension. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
            return null;
        }

        Path filePath = customDir.resolve(fileName);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            plugin.getLogger().info("Custom file not found: " + filePath);
            return null;
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (!isValidFileContent(fileName, content)) {
                plugin.getLogger().warning("Skipped custom file " + fileName + " due to malformed content.");
                return null;
            }

            if (placeholders != null && !placeholders.isEmpty()) {
                content = replacePlaceholders(content, placeholders);
            }

//            plugin.getLogger().info("Loaded custom file: " + fileName);
            return content;
        } catch (IOException e) {
            plugin.getLogger().severe("Error reading custom file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean isValidFileContent(String fileName, String content) {
        if (content == null || content.trim().isEmpty()) {
            plugin.getLogger().warning("Empty or null content in file: " + fileName);
            return false;
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        try {
            switch (extension) {
                case "html":
                    return isValidHtml(content);
                case "css":
                    return isValidCss(content);
                case "js":
                    return isValidJavaScript(content);
                default:
                    plugin.getLogger().warning("Unsupported file type: " + fileName);
                    return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Validation error for " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isValidHtml(String content) {
        boolean hasHtml = content.toLowerCase().contains("<html") || content.toLowerCase().contains("<!doctype");
        if (!hasHtml) {
            plugin.getLogger().warning("Invalid HTML: Missing <html> or DOCTYPE in content starting with: " +
                    content.substring(0, Math.min(50, content.length())) + "...");
            return false;
        }
        return true;
    }

    private boolean isValidCss(String content) {
        return isBalancedBraces(content, '{', '}');
    }

    private boolean isValidJavaScript(String content) {
        return isBalancedBraces(content, '{', '}') && isBalancedBraces(content, '(', ')');
    }

    private boolean isBalancedBraces(String content, char open, char close) {
        int count = 0;
        boolean inString = false;
        char stringChar = '\0';
        boolean inComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (!inComment && !inLineComment) {
                if (inString) {
                    if (c == stringChar && (i == 0 || content.charAt(i - 1) != '\\')) {
                        inString = false;
                    }
                    continue;
                } else if (c == '"' || c == '\'' || c == '`') {
                    inString = true;
                    stringChar = c;
                    continue;
                }
            }

            if (i + 1 < content.length()) {
                if (c == '/' && content.charAt(i + 1) == '*' && !inString && !inComment && !inLineComment) {
                    inComment = true;
                    i++;
                    continue;
                } else if (c == '/' && content.charAt(i + 1) == '/' && !inString && !inComment && !inLineComment) {
                    inLineComment = true;
                    i++;
                    continue;
                }
            }
            if (inComment && c == '*' && i + 1 < content.length() && content.charAt(i + 1) == '/') {
                inComment = false;
                i++;
                continue;
            }
            if (inLineComment && c == '\n') {
                inLineComment = false;
                continue;
            }

            if (!inString && !inComment && !inLineComment) {
                if (c == open) {
                    count++;
                } else if (c == close) {
                    count--;
                    if (count < 0) {
                        plugin.getLogger().warning("Unmatched closing " + close + " in content starting with: " +
                                content.substring(0, Math.min(50, content.length())) + "...");
                        return false;
                    }
                }
            }
        }

        if (count != 0) {
            plugin.getLogger().warning("Unbalanced " + open + "/" + close + " in content starting with: " +
                    content.substring(0, Math.min(50, content.length())) + "...");
            return false;
        }
        if (inString || inComment || inLineComment) {
            plugin.getLogger().warning("Unclosed string or comment in content starting with: " +
                    content.substring(0, Math.min(50, content.length())) + "...");
            return false;
        }
        return true;
    }

    private String replacePlaceholders(String content, Map<String, String> placeholders) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(content, lastEnd, matcher.start());
            String placeholder = matcher.group(1);
            String replacement = placeholders.getOrDefault(placeholder, matcher.group(0));
            result.append(replacement);
            lastEnd = matcher.end();
        }
        result.append(content, lastEnd, content.length());

        return result.toString();
    }
}