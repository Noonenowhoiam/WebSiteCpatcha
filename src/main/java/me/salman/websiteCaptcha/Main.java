package me.salman.websiteCaptcha;

import me.salman.websiteCaptcha.Api.VoidWorldManager;
import me.salman.websiteCaptcha.Command.WebCaptchaCommand;
import me.salman.websiteCaptcha.Database.SQLiteManager;
import me.salman.websiteCaptcha.Listeners.*;
import me.salman.websiteCaptcha.Manager.InventoryManager;
import me.salman.websiteCaptcha.Manager.Loader.CustomFileLoader;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import me.salman.websiteCaptcha.Manager.WebsiteFileManager;
import me.salman.websiteCaptcha.Website.WebServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class Main extends JavaPlugin {
    private WebServer webServer;
    private VerificationManager verificationManager;
    private SQLiteManager sqliteManager;
    private InventoryManager inventoryManager;
    private WebsiteFileManager websiteFileManager;
    private CustomFileLoader customFileLoader;
    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("Starting WebsiteCaptcha plugin initialization...");

        websiteFileManager = new WebsiteFileManager(this);
        customFileLoader = new CustomFileLoader(this);
        if (!websiteFileManager.setupWebsiteFolder()) {
            getLogger().severe("Failed to set up website files. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Map<String, String> customFiles = customFileLoader.loadCustomFiles();
        if (!customFiles.isEmpty()) {
            getLogger().info("Loaded " + customFiles.size() + " custom file(s).");
            for (String fileName : customFiles.keySet()) {
                getLogger().info("Loaded " + fileName + " file");
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");
            getLogger().info("SQLite JDBC driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            getLogger().severe("SQLite JDBC driver not found! Ensure sqlite-jdbc is in your dependencies.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            sqliteManager = SQLiteManager.getInstance(this);
            getLogger().info("SQLiteManager initialized successfully: " + sqliteManager.isConnectionValid());
        } catch (Exception e) {
            getLogger().severe("Failed to initialize SQLiteManager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        inventoryManager = new InventoryManager();
        try {
            verificationManager = new VerificationManager(sqliteManager, this);
            getLogger().info("VerificationManager initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize VerificationManager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            webServer = new WebServer(verificationManager);
            webServer.startServer();
            int port = getConfig().getInt("Web.port", 8080);
            String host = getConfig().getString("Web.host", "localhost");
            getLogger().info("Web server started on " + host + ":" + port);
        } catch (IOException e) {
            getLogger().severe("Failed to start WebServer: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        WebCaptchaCommand webCaptchaCommand = new WebCaptchaCommand(verificationManager);
        getCommand("webcaptcha").setExecutor(webCaptchaCommand);
        getCommand("webcaptcha").setTabCompleter(webCaptchaCommand);

        VoidWorldManager.initializePacketBlocking(this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(verificationManager, inventoryManager), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(verificationManager), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandPreprocessListener(verificationManager), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(verificationManager), this);
        getServer().getPluginManager().registerEvents(new PlayerActionListener(verificationManager), this);

        getLogger().info("WebsiteCaptcha Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stopServer();
            getLogger().info("Web server stopped!");
        }
        if (sqliteManager != null) {
            sqliteManager.closeConnection();
            getLogger().info("SQLite connection closed!");
        }
        getLogger().info("WebsiteCaptcha Plugin disabled!");
    }

    public static Main getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Plugin instance is null; plugin may not be enabled yet.");
        }
        return instance;
    }

    public void reloadConfigFile() {
        reloadConfig();
        getLogger().info("Plugin configuration reloaded successfully!");
    }

    public VerificationManager getVerificationManager() {
        return verificationManager;
    }

    public WebsiteFileManager getWebsiteFileManager() {
        return websiteFileManager;
    }

    public CustomFileLoader getCustomFileLoader() {
        return customFileLoader;
    }

    public SQLiteManager getSQLiteManager() {
        return sqliteManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}