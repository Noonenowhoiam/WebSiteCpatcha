package me.salman.websiteCaptcha.Api;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import me.salman.websiteCaptcha.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class VoidWorldManager implements Listener {

    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private static final ConcurrentHashMap<UUID, Boolean> limboStatus = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, BukkitTask> titleTasks = new ConcurrentHashMap<>();
    private static final Location LIMBO_LOCATION = new Location(Bukkit.getWorlds().get(0), 2000, 1000, 2000);

    private static boolean sendTitle = false;
    private static boolean sendActionBar = false;
    private static boolean sendChatMessage = false;

    private static String titleText = "&bWelcome to Limbo";
    private static String subtitleText = "&7Please wait...";
    private static String actionBarText = "&eYou are in Limbo...";
    private static String chatMessageText = "&cYou have entered Limbo.";

    public static void initializePacketBlocking(Plugin plugin) {
        protocolManager.addPacketListener(new PacketAdapter(plugin,
                PacketType.Play.Server.BLOCK_CHANGE,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null || !player.isOnline()) return;
                if (limboStatus.getOrDefault(player.getUniqueId(), false) && !isAllowedPacket(event.getPacketType())) {
                    event.setCancelled(true);
                }
            }
        });
        Bukkit.getPluginManager().registerEvents(new VoidWorldManager(), plugin);
    }

    private static boolean isAllowedPacket(PacketType packetType) {
        return packetType == PacketType.Play.Server.KEEP_ALIVE ||
                packetType == PacketType.Play.Server.PLAYER_INFO ||
                packetType == PacketType.Play.Server.NAMED_ENTITY_SPAWN ||
                packetType == PacketType.Play.Server.ENTITY_METADATA;
    }

    public static void sendToLimbo(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID playerUUID = player.getUniqueId();
        if (limboStatus.getOrDefault(playerUUID, false)) {
            Bukkit.getLogger().info("[Limbo] Player " + player.getName() + " is already in Limbo.");
            return;
        }

        limboStatus.put(playerUUID, true);
        player.teleport(LIMBO_LOCATION);
        player.getWorld().loadChunk(LIMBO_LOCATION.getBlockX() >> 4, LIMBO_LOCATION.getBlockZ() >> 4);
        sendEmptyChunk(player);
        sendInitialMessage(player);
        keepTitleAndActionBar(player);
    }

    private static void sendEmptyChunk(Player player) {
        try {
            PacketContainer emptyChunkPacket = protocolManager.createPacket(PacketType.Play.Server.MAP_CHUNK);
            emptyChunkPacket.getIntegers()
                    .write(0, LIMBO_LOCATION.getBlockX() >> 4)
                    .write(1, LIMBO_LOCATION.getBlockZ() >> 4);
            emptyChunkPacket.getBooleans().write(0, true);
            byte[] chunkData = new byte[256];
            emptyChunkPacket.getSpecificModifier(byte[].class).writeSafely(0, chunkData);
            protocolManager.sendServerPacket(player, emptyChunkPacket);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Limbo] Error sending empty chunk to " + player.getName(), e);
        }
    }

    public static void sendToNormalWorld(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID playerUUID = player.getUniqueId();
        limboStatus.put(playerUUID, false);
        player.resetTitle();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        BukkitTask task = titleTasks.remove(playerUUID);
        if (task != null) {
            task.cancel();
//            Bukkit.getLogger().info("[Limbo] Canceled title/action bar task for " + player.getName());
        }

        World mainWorld = Bukkit.getWorlds().get(0);
        if (mainWorld != null) {
            player.teleport(mainWorld.getSpawnLocation());
        } else {
            Bukkit.getLogger().warning("[Limbo] Main world is null, cannot teleport " + player.getName() + " back.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerFromLimbo(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        removePlayerFromLimbo(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (!limboStatus.getOrDefault(playerUUID, false)) {
            sendToNormalWorld(player);
        }
    }

    private static void removePlayerFromLimbo(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        if (limboStatus.containsKey(playerUUID)) {
            limboStatus.remove(playerUUID);
            BukkitTask task = titleTasks.remove(playerUUID);
            if (task != null) {
                task.cancel();
                Bukkit.getLogger().info("[Limbo] Canceled title/action bar task for " + player.getName() + " on disconnect.");
            }
            Bukkit.getLogger().info("[Limbo] Removed " + player.getName() + " from Limbo on disconnect.");
        }
    }

    private static void keepTitleAndActionBar(Player player) {
        UUID playerUUID = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!limboStatus.containsKey(playerUUID) || !player.isOnline()) {
                    cancel();
                    titleTasks.remove(playerUUID);
                    return;
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendTitleAndActionBar(player);
                    }
                }.runTask(Main.getInstance());
            }
        }.runTaskTimerAsynchronously(Main.getInstance(), 0L, 10L);
        titleTasks.put(playerUUID, task);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        if (event.getWorld().equals(LIMBO_LOCATION.getWorld()) && limboStatus.containsValue(true)) {
            event.setCancelled(true);
            Bukkit.getLogger().warning("[Limbo] Prevented Limbo world from unloading while players are inside.");
        }
    }

    private static void sendTitleAndActionBar(Player player) {
        if (sendTitle) {
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', titleText),
                    ChatColor.translateAlternateColorCodes('&', subtitleText), 0, 100, 0);
        }

        if (sendActionBar) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', actionBarText)));
        }
    }

    private static void sendInitialMessage(Player player) {
        if (sendChatMessage) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessageText));
        }
    }

    public static void setSendTitle(boolean value) {
        sendTitle = value;
    }

    public static void setSendActionBar(boolean value) {
        sendActionBar = value;
    }

    public static void setSendChatMessage(boolean value) {
        sendChatMessage = value;
    }

    public static void setTitleText(String title, String subtitle) {
        titleText = title;
        subtitleText = subtitle;
    }

    public static void setActionBarText(String text) {
        actionBarText = text;
    }

    public static void setChatMessageText(String text) {
        chatMessageText = text;
    }
}