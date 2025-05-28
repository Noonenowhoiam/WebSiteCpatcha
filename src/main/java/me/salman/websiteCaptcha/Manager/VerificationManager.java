package me.salman.websiteCaptcha.Manager;

import me.salman.websiteCaptcha.Database.SQLiteManager;
import me.salman.websiteCaptcha.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public class VerificationManager {
    private final SQLiteManager sqliteManager;
    private final HashSet<UUID> whitelist = new HashSet<>();
    private final Main plugin;

    public VerificationManager(SQLiteManager sqliteManager, Main plugin) {
        if (sqliteManager == null) {
            throw new IllegalArgumentException("SQLiteManager cannot be null!");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null!");
        }
        this.sqliteManager = sqliteManager;
        this.plugin = plugin;
        plugin.getLogger().info("VerificationManager initialized with SQLiteManager.");
    }

    public void setPlayerVerified(UUID uuid, boolean verified) {
        long currentTime = System.currentTimeMillis();
        sqliteManager.setPlayerVerified(uuid, verified, currentTime);
        plugin.getLogger().info("Set verification for UUID " + uuid + " to " + verified + " with time " + currentTime);
    }

    public boolean isPlayerVerified(UUID uuid) {
        if (isPlayerWhitelisted(uuid)) {
            return true;
        }
        return sqliteManager.isPlayerVerified(uuid);
    }

    @Deprecated
    public void setPlayerRecentlyVerified(UUID uuid, boolean recentlyVerified) {
        if (recentlyVerified) {
            long currentTime = System.currentTimeMillis();
            sqliteManager.setLastVerificationTime(uuid, currentTime);
            plugin.getLogger().info("Set last verification time for UUID " + uuid + " to " + currentTime);
        }
    }

    public boolean isPlayerRecentlyVerified(UUID uuid) {
        Long lastTime = sqliteManager.getLastVerificationTime(uuid);
        if (lastTime == null) return false;
        long currentTime = System.currentTimeMillis();
        long gracePeriod = plugin.getConfig().getLong("options.verification_grace_period", 24 * 60 * 60 * 1000);
        return (currentTime - lastTime) <= gracePeriod;
    }

    public void clearPlayerVerification(UUID uuid) {
        sqliteManager.clearPlayerVerification(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.kickPlayer("§cYour verification has been cleared. Please re-verify to join.");
        }
    }

    public void addPlayerToWhitelist(UUID uuid) {
        whitelist.add(uuid);
    }

    public void removePlayerFromWhitelist(UUID uuid) {
        whitelist.remove(uuid);
    }

    public boolean isPlayerWhitelisted(UUID uuid) {
        return whitelist.contains(uuid);
    }

    public HashSet<UUID> getWhitelist() {
        return new HashSet<>(whitelist);
    }

    public void removePlayerVerification(UUID uuid) {
        clearPlayerVerification(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.kickPlayer("§cYour verification has been cleared. Please re-verify to join.");
        }
    }

    public void setVerificationTask(UUID uuid, int taskId) {
        sqliteManager.setVerificationTask(uuid, taskId);
    }

    public Integer getVerificationTask(UUID uuid) {
        return sqliteManager.getVerificationTask(uuid);
    }

    public void removeVerificationTask(UUID uuid) {
        sqliteManager.removeVerificationTask(uuid);
    }

    public Long getLastVerificationTime(UUID uuid) {
        return sqliteManager.getLastVerificationTime(uuid);
    }

    public Main getPlugin() {
        return plugin;
    }
}