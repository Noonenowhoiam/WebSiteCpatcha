package me.salman.websiteCaptcha.Listeners;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.salman.websiteCaptcha.Api.VoidWorldManager;
import me.salman.websiteCaptcha.Manager.InventoryManager;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final VerificationManager verificationManager;
    private final InventoryManager inventoryManager;
    private final ProtocolManager protocolManager;



    public PlayerJoinListener(VerificationManager verificationManager, InventoryManager inventoryManager) {
        this.verificationManager = verificationManager;
        this.inventoryManager = inventoryManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String playerIP = player.getAddress().getAddress().getHostAddress();
        String playerName = player.getName();
        String playerVersion = getPlayerVersion(player);

        verificationManager.getPlugin().getLogger().info(String.format(
                "[WebSiteCaptcha]: [/%s|%s|%s] <-> VirtualConnector joined to the filter",
                playerIP, playerName, playerVersion
        ));
//        verificationManager.getPlugin().getLogger().info("PlayerJoinListener started for UUID " + playerUUID + " at " + System.currentTimeMillis() + " on thread " + Thread.currentThread().getName());

        long currentTime = System.currentTimeMillis();
        Long lastTime = verificationManager.getLastVerificationTime(playerUUID);
        boolean isVerified = verificationManager.isPlayerVerified(playerUUID);
        long gracePeriod = verificationManager.getPlugin().getConfig().getLong("options.verification_grace_period", 24 * 60 * 60 * 1000); // Default: 24 hours

//        verificationManager.getPlugin().getLogger().info("Checking verification status for UUID " + playerUUID + ": isVerified=" + isVerified + ", lastTime=" + lastTime + ", gracePeriod=" + gracePeriod);

        if (isVerified && lastTime != null && (currentTime - lastTime) <= gracePeriod) {
            player.sendMessage(formatMessage(verificationManager.getPlugin().getConfig().getString("messages.already_verified")));
            VoidWorldManager.sendToNormalWorld(player);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            if (verificationManager.getPlugin().getConfig().getBoolean("options.hide_inventory")) {
                inventoryManager.restoreInventory(player);
            }
            player.setLevel(0);
            player.setExp(0.0f);
//            verificationManager.getPlugin().getLogger().info("Player " + playerUUID + " is verified, sent to normal world, display settings reset.");
            return;
        }

//        verificationManager.getPlugin().getLogger().info("Player " + playerUUID + " not verified, sending to Limbo.");
        VoidWorldManager.sendToLimbo(player);

        // Set game mode from config
        String gameModeString = verificationManager.getPlugin().getConfig().getString("options.game_mode", "ADVENTURE").toUpperCase();
        GameMode gameMode;
        try {
            gameMode = GameMode.valueOf(gameModeString);
        } catch (IllegalArgumentException e) {
            gameMode = GameMode.ADVENTURE;
            verificationManager.getPlugin().getLogger().warning("Invalid game_mode '" + gameModeString + "' in config.yml. Defaulting to ADVENTURE.");
        }
        player.setGameMode(gameMode);
//        verificationManager.getPlugin().getLogger().info("Set game mode for UUID " + playerUUID + " to " + gameMode);

        player.setAllowFlight(true);
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR || !verificationManager.getPlugin().getConfig().getBoolean("options.prevent_unverified_player_movement", true)) {
            player.setFlying(true);
        }
//        verificationManager.getPlugin().getLogger().info("Set allow_flight for UUID " + playerUUID + " to true, flying=" + player.isFlying());

        Integer existingTaskId = verificationManager.getVerificationTask(playerUUID);
        if (existingTaskId != null) {
//            verificationManager.getPlugin().getLogger().info("Cancelling existing task " + existingTaskId + " for UUID " + playerUUID);
            Bukkit.getScheduler().cancelTask(existingTaskId);
            verificationManager.removeVerificationTask(playerUUID);
        }

        boolean sendTitle = verificationManager.getPlugin().getConfig().getBoolean("options.SendTitle", false);
        String titleText = formatMessage(verificationManager.getPlugin().getConfig().getString("options.title", "&aWelcome To Server"));
        String subtitleText = formatMessage(verificationManager.getPlugin().getConfig().getString("options.subtitle", "&aThanks for using my plugin"));
        VoidWorldManager.setSendTitle(sendTitle);
        VoidWorldManager.setTitleText(titleText, subtitleText);
//        verificationManager.getPlugin().getLogger().info("Set SendTitle=" + sendTitle + ", title='" + titleText + "', subtitle='" + subtitleText + "' for UUID " + playerUUID);

        boolean sendActionBar = verificationManager.getPlugin().getConfig().getBoolean("options.SendActionBar", false);
        String actionBarText = formatMessage(verificationManager.getPlugin().getConfig().getString("options.actionBar", "&ePlease verify to join!"));
        VoidWorldManager.setSendActionBar(sendActionBar);
        VoidWorldManager.setActionBarText(actionBarText);
//        verificationManager.getPlugin().getLogger().info("Set SendActionBar=" + sendActionBar + ", actionBar='" + actionBarText + "' for UUID " + playerUUID);

        boolean sendChatMessage = verificationManager.getPlugin().getConfig().getBoolean("options.SendChatMessage", false);
        String chatMessageText = formatMessage(verificationManager.getPlugin().getConfig().getString("options.chatMessage", "&eClick the link to verify!"));
        VoidWorldManager.setSendChatMessage(sendChatMessage);
        VoidWorldManager.setChatMessageText(chatMessageText);
//        verificationManager.getPlugin().getLogger().info("Set SendChatMessage=" + sendChatMessage + ", chatMessage='" + chatMessageText + "' for UUID " + playerUUID);

        int port = verificationManager.getPlugin().getConfig().getInt("Web.port", 8080);
        String host = verificationManager.getPlugin().getConfig().getString("Web.host", "localhost");
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }
        if (!host.endsWith(":")) {
            host += ":";
        }
        String verificationLink = "http://" + host + port + "/captcha?uuid=" + playerUUID;
        TextComponent message = new TextComponent(formatMessage(verificationManager.getPlugin().getConfig().getString("messages.verification_prompt")));
        TextComponent link = new TextComponent(formatMessage(verificationManager.getPlugin().getConfig().getString("messages.verification_link_text")));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, verificationLink));
        message.addExtra(link);
        player.spigot().sendMessage(message);

        boolean applyBlindness = verificationManager.getPlugin().getConfig().getBoolean("options.apply_blindness", true);
//        verificationManager.getPlugin().getLogger().info("Apply blindness for UUID " + playerUUID + ": " + applyBlindness);
        if (applyBlindness) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
//            verificationManager.getPlugin().getLogger().info("Blindness effect applied to UUID " + playerUUID);
        }

        if (verificationManager.getPlugin().getConfig().getBoolean("options.hide_inventory")) {
            inventoryManager.hideInventory(player);
            player.sendMessage(formatMessage(verificationManager.getPlugin().getConfig().getString("messages.inventory_hidden")));
        }

        int timeoutSeconds = verificationManager.getPlugin().getConfig().getInt("options.verification_timeout", 60);
        String kickMessage = formatMessage(verificationManager.getPlugin().getConfig().getString("messages.verification_timeout_kick", "&cYou took too long to verify!"));

        player.setLevel(timeoutSeconds);
        player.setExp(1.0f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(verificationManager.getPlugin(), new Runnable() {
            private int secondsRemaining = timeoutSeconds;
            private float expProgress = 1.0f;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    Integer taskId = verificationManager.getVerificationTask(playerUUID);
                    if (taskId != null) {
//                        verificationManager.getPlugin().getLogger().info("Cancelling task " + taskId + " for UUID " + playerUUID + " due to player disconnect.");
                        Bukkit.getScheduler().cancelTask(taskId);
                        verificationManager.removeVerificationTask(playerUUID);
                    }
                    return;
                }
                // Reset the things after  verification complate
                if (verificationManager.isPlayerVerified(playerUUID)) {
                    player.setLevel(0);
                    player.setExp(0.0f);
                    Integer taskId = verificationManager.getVerificationTask(playerUUID);
                    if (taskId != null) {
//                        verificationManager.getPlugin().getLogger().info("Cancelling task " + taskId + " for UUID " + playerUUID + " after verification.");
                        Bukkit.getScheduler().cancelTask(taskId);
                        verificationManager.removeVerificationTask(playerUUID);
                    }
                    return;
                }

                secondsRemaining--;
                // Prevent negative levels
                if (secondsRemaining >= 0) {
                    player.setLevel(secondsRemaining);
                }

                float decrement = 1.0f / timeoutSeconds;
                expProgress -= decrement;
                if (expProgress < 0.0f) expProgress = 0.0f;
                player.setExp(expProgress);

                if (secondsRemaining <= 0) {
                    player.kickPlayer(kickMessage);
                    Integer taskId = verificationManager.getVerificationTask(playerUUID);
                    if (taskId != null) {
//                        verificationManager.getPlugin().getLogger().info("Cancelling task " + taskId + " for UUID " + playerUUID + " due to timeout kick.");
                        Bukkit.getScheduler().cancelTask(taskId);
                        verificationManager.removeVerificationTask(playerUUID);
                    }
                }
            }
        }, 0L, 20L);

        verificationManager.setVerificationTask(playerUUID, task.getTaskId());
    }

    private String getPlayerVersion(Player player) {
        int protocolVersion = protocolManager.getProtocolVersion(player);
        return switch (protocolVersion) {
            case 47 -> "1.8";
            case 107 -> "1.9";
            case 108 -> "1.9.1";
            case 109 -> "1.9.2";
            case 110 -> "1.9.3/1.9.4";
            case 210 -> "1.10";
            case 315 -> "1.11";
            case 316 -> "1.11.1/1.11.2";
            case 335 -> "1.12";
            case 338 -> "1.12.1";
            case 340 -> "1.12.2";
            case 393 -> "1.13";
            case 401 -> "1.13.1";
            case 404 -> "1.13.2";
            case 477 -> "1.14";
            case 480 -> "1.14.1";
            case 485 -> "1.14.2";
            case 490 -> "1.14.3";
            case 498 -> "1.14.4";
            case 573 -> "1.15";
            case 575 -> "1.15.1";
            case 578 -> "1.15.2";
            case 735 -> "1.16";
            case 736 -> "1.16.1";
            case 751 -> "1.16.2";
            case 753 -> "1.16.3";
            case 754 -> "1.16.4/1.16.5";
            case 755 -> "1.17";
            case 756 -> "1.17.1";
            case 757 -> "1.18";
            case 758 -> "1.18.2";
            case 759 -> "1.19";
            case 760 -> "1.19.1/1.19.2";
            case 761 -> "1.19.3";
            case 762 -> "1.19.4";
            case 763 -> "1.20";
            case 764 -> "1.20.1";
            case 765 -> "1.20.2";
            case 766 -> "1.20.3";
            case 767 -> "1.21";
            case 768 -> "1.21.1";
            case 769 -> "1.21.2";
            case 770 -> "1.21.3";
            case 771 -> "1.21.4";
            default -> "Unknown (" + protocolVersion + ")";
        };
    }

}