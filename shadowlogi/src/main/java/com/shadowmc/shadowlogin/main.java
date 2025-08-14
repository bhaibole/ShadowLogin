// Main package for your plugin.
package com.shadowmc.shadowlogin;

// Bukkit/Spigot Imports
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

// Java Imports
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// You will likely need additional imports for:
// - Database (e.g., java.sql.*, HikariCP)
// - Hashing (e.g., org.mindrot.jbcrypt.BCrypt)
// - Configuration management (e.g., Spigot config methods)
// - PlaceholderAPI integration

public class ShadowLogin extends JavaPlugin implements Listener {

    private static ShadowLogin instance; // Singleton instance

    // Configuration values (loaded from config.yml)
    private String prefix;
    private String authWorldName;
    private String redirectWorldName;
    private String registerTitle;
    private String registerSubtitle;
    private String loginTitle;
    private String loginSubtitle;
    private String premiumRegisterTitle;
    private String premiumRegisterSubtitle;
    private String premiumLoginTitle;
    private String premiumLoginSubtitle;

    private int registerTimeSeconds = 60; // Example countdown time
    private int loginTimeSeconds = 60;    // Example countdown time

    // Keep track of players who need to register/login
    private Set<UUID> unauthenticatedPlayers = ConcurrentHashMap.newKeySet();
    // Map to store countdown tasks for players
    private Map<UUID, BukkitRunnable> countdownTasks = new ConcurrentHashMap<>();
    // A simplified in-memory "database" for demonstration (replace with a real DB!)
    private Map<UUID, String> registeredPlayers = new ConcurrentHashMap<>();
    private Set<UUID> premiumPlayers = ConcurrentHashMap.newKeySet(); // Track premium status

    public static ShadowLogin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        // Save default config if not exists
        saveDefaultConfig();
        // Load configurations
        loadConfigValues();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands
        getCommand("register").setExecutor(this);
        getCommand("login").setExecutor(this);

        // PlaceholderAPI integration (if PlaceholderAPI is present)
        // You'd typically check for PlaceholderAPI and register your expansion here
        // if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        //     new ShadowLoginPlaceholderExpansion(this).register();
        // }

        getLogger().info(prefix + "ShadowLogin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel all pending countdown tasks
        countdownTasks.values().forEach(BukkitRunnable::cancel);
        countdownTasks.clear();

        // Save any pending data to your database if necessary

        getLogger().info(prefix + "ShadowLogin has been disabled!");
    }

    private void loadConfigValues() {
        // Get values from your config.yml (using the example structure from previous responses)
        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&b&lShadowLogin &8>> &f"));
        this.authWorldName = getConfig().getString("auth-world", "auth");
        this.redirectWorldName = getConfig().getString("redirect-world", "world");

        this.registerTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("register-title.title", "&#008EFF&lS&#008EFF&lʜ&#008EFF&lᴏ&#008EFF&lᴅ&#008EFF&lᴏ&#008EFF&lᴡ&#008EFF&lM&#008EFF&lC"));
        this.registerSubtitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("register-title.subtitle", "&#FFFD00Y&#FFFD00ᴏ&#FFFD00ᴜ &#FFFD00ʜ&#FFFD00ᴀ&#FFFD00ᴠ&#FFFD00ᴇ &#FFFD00(&#FFFD00ᴘ&#FFFD00ʟ&#FFFD00ᴀ&#FFFD00ᴄ&#FFFD00ᴇ&#FFFD00ʜ&#FFFD00ᴏ&#FFFD00ʟ&#FFFD00ᴅ&#FFFD00ᴇ&#FFFD00ʀ&#FFFD00)&#FFFD00, &#FFFD00T&#FFFD00ᴏ &#FFFD00ʀ&#FFFD00ᴇ&#FFFD00ɢ&#FFFD00ɪ&#FFFD00ѕ&#FFFD00ᴛ&#FFFD00ᴇ&#FFFD00ʀ"));
        // Load fadeIn, stay, fadeOut as well

        this.loginTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("login-title.title", "&#00FF23&lL&#00FF23&lᴏ&#00FF23&lɢ&#00FF23&lɪ&#00FF23&lɴ"));
        this.loginSubtitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("login-title.subtitle", "&#FFFD00Y&#FFFD00ᴏ&#FFFD00ᴜ &#FFFD00ʜ&#FFFD00ᴀ&#FFFD00ᴠ&#FFFD00ᴇ &#FFFD00(&#FFFD00ᴘ&#FFFD00ʟ&#FFFD00ᴀ&#FFFD00ᴄ&#FFFD00ᴇ&#FFFD00ʜ&#FFFD00ᴏ&#FFFD00ʟ&#FFFD00ᴅ&#FFFD00ᴇ&#FFFD00ʀ&#FFFD00)&#FFFD00, &#FFFD00T&#FFFD00ᴏ &#FFFD00ʟ&#FFFD00ᴏ&#FFFD00ɢ&#FFFD00ɪ&#FFFD00ɴ"));
        // Load fadeIn, stay, fadeOut as well

        this.premiumRegisterTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("premium-register-title.title", "&#00FF0AY&#00FF0Aᴏ&#00FF0Aᴜ &#00FF0Aʜ&#00FF0Aᴀ&#00FF0Aᴠ&#00FF0Aᴇ &#00FF0Aʙ&#00FF0Aᴇ&#00FF0Aᴇ&#00FF0Aɴ &#00FF0Aᴀ&#00FF0Aᴜ&#00FF0Aᴛ&#00FF0Aᴏ&#00FF0Aᴍ&#00FF0Aᴀ&#00FF0Aᴛ&#00FF0Aɪ&#00FF0Aᴄ&#00FF0Aᴀ&#00FF0Aʟ&#00FF0Aʟ&#00FF0Aʏ &#00FF0Aʀ&#00FF0Aᴇ&#00FF0Aɢ&#00FF0Aɪ&#00FF0Aѕ&#00FF0Aᴛ&#00FF0Aᴇ&#00FF0Aʀ&#00FF0Aᴇ&#00FF0Aᴅ"));
        this.premiumRegisterSubtitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("premium-register-title.subtitle", "&7Welcome to the server."));

        this.premiumLoginTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("premium-login-title.title", "&#00FF0AY&#00FF0Aᴏ&#00FF0Aᴜ &#00FF0Aʜ&#00FF0Aᴀ&#00FF0Aᴠ&#00FF0Aᴇ &#00FF0Aʙ&#00FF0Aᴇ&#00FF0Aᴇ&#00FF0Aɴ &#00FF0Aᴀ&#00FF0Aᴜ&#00FF0Aᴛ&#00FF0Aᴏ&#00FF0Aᴍ&#00FF0Aᴀ&#00FF0Aᴛ&#00FF0Aɪ&#00FF0Aᴄ&#00FF0Aᴀ&#00FF0Aʟ&#00FF0Aʟ&#00FF0Aʏ &#00FF0Aʟ&#00FF0Aᴏ&#00FF0Aɢ&#00FF0Aɢ&#00FF0Aᴇ&#00FF0Aᴅ &#00FF0Aɪ&#00FF0Aɴ"));
        this.premiumLoginSubtitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("premium-login-title.subtitle", "&7Enjoy your game."));

        // You'd also load registerTimeSeconds and loginTimeSeconds from config
    }

    /**
     * Sends a title to the player. This method would need
     * significant version-specific logic or reflection for 1.8.8-1.21.5 support.
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // For simplicity, using Bukkit's built-in method (available 1.11+)
        // For 1.8.8-1.10.2, you'd send packets directly using NMS/reflection.
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Handles player pre-login event to check premium status.
     * This is an asynchronous event, so database lookups are safe here.
     */
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        // This is where you'd determine if the player is premium.
        // For BungeeCord/Velocity, you might get this information via plugin messages
        // or check Mojang's API (but that's rate-limited and not ideal for every join).
        // For simplicity, let's assume UUID is a direct indicator of premium for now.
        boolean isPremium = (uuid.version() == 4); // Standard UUIDs are version 4. Cracked UUIDs are often version 3.
                                                    // This is a common, but not foolproof, check.
                                                    // A better check involves communicating with your proxy or Mojang API.

        if (isPremium) {
            premiumPlayers.add(uuid);
            // Check if player is already registered in your database
            if (!isPlayerRegistered(uuid)) { // Implement this database check
                // Automatically register premium player
                getLogger().info(prefix + "Automatically registering premium player: " + name);
                // In a real plugin, save their UUID to your database here.
                registeredPlayers.put(uuid, "premium_auto"); // Placeholder password
            }
        } else {
            // Cracked player
            getLogger().info(prefix + "Cracked player detected: " + name);
        }
    }

    /**
     * Handles player join event.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if premium
        if (premiumPlayers.contains(uuid)) {
            // Already handled by pre-login, just send login message and teleport
            getLogger().info(prefix + "Premium player logged in: " + player.getName());
            sendTitle(player, premiumLoginTitle, premiumLoginSubtitle, 10, 70, 20);
            teleportPlayerToSpawn(player);
            return; // Exit as premium player handled
        }

        // Cracked Player Logic
        // Check if already registered
        if (isPlayerRegistered(uuid)) { // Implement this database check
            // Player needs to log in
            unauthenticatedPlayers.add(uuid);
            player.sendMessage(prefix + ChatColor.YELLOW + "Please log in using /login <password>");
            teleportPlayerToAuthWorld(player);
            startLoginCountdown(player);
        } else {
            // Player needs to register
            unauthenticatedPlayers.add(uuid);
            player.sendMessage(prefix + ChatColor.YELLOW + "Please register using /register <password> <password>");
            teleportPlayerToAuthWorld(player);
            startRegisterCountdown(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Clear player from unauthenticated set and cancel their countdown
        unauthenticatedPlayers.remove(uuid);
        if (countdownTasks.containsKey(uuid)) {
            countdownTasks.get(uuid).cancel();
            countdownTasks.remove(uuid);
        }
        premiumPlayers.remove(uuid); // Clear premium status on quit
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (!unauthenticatedPlayers.contains(uuid)) {
            player.sendMessage(prefix + ChatColor.GREEN + "You are already logged in!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("register")) {
            if (args.length != 2) {
                player.sendMessage(prefix + ChatColor.RED + "Usage: /register <password> <confirm_password>");
                return true;
            }
            if (!args[0].equals(args[1])) {
                player.sendMessage(prefix + ChatColor.RED + "Passwords do not match!");
                return true;
            }
            if (args[0].length() < 6) { // Example password length requirement
                player.sendMessage(prefix + ChatColor.RED + "Password must be at least 6 characters long.");
                return true;
            }

            // In a real plugin: Hash the password (e.g., String hashedPassword = BCrypt.hashpw(args[0], BCrypt.gensalt());)
            String hashedPassword = args[0]; // Placeholder

            // Save to your database
            registeredPlayers.put(uuid, hashedPassword); // Replace with real DB operation

            player.sendMessage(prefix + ChatColor.GREEN + "You have successfully registered and logged in!");
            completeAuthentication(player);
            return true;

        } else if (command.getName().equalsIgnoreCase("login")) {
            if (args.length != 1) {
                player.sendMessage(prefix + ChatColor.RED + "Usage: /login <password>");
                return true;
            }
            String enteredPassword = args[0];

            // In a real plugin: Retrieve hashed password from DB and compare (e.g., BCrypt.checkpw(enteredPassword, storedHashedPassword);)
            if (registeredPlayers.containsKey(uuid) && registeredPlayers.get(uuid).equals(enteredPassword)) { // Placeholder check
                player.sendMessage(prefix + ChatColor.GREEN + "You have successfully logged in!");
                completeAuthentication(player);
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Incorrect password!");
            }
            return true;
        }
        return false;
    }

    /**
     * Teleports player to the authentication world.
     */
    private void teleportPlayerToAuthWorld(Player player) {
        World authWorld = Bukkit.getWorld(authWorldName);
        if (authWorld == null) {
            player.kickPlayer(prefix + ChatColor.RED + "Authentication world not found! Please contact an admin.");
            getLogger().severe("Authentication world '" + authWorldName + "' not found! Check your config.");
            return;
        }
        player.teleport(authWorld.getSpawnLocation());
    }

    /**
     * Teleports player to the main game world (spawn/lobby).
     */
    private void teleportPlayerToSpawn(Player player) {
        World redirectWorld = Bukkit.getWorld(redirectWorldName);
        if (redirectWorld == null) {
            player.kickPlayer(prefix + ChatColor.RED + "Redirect world not found! Please contact an admin.");
            getLogger().severe("Redirect world '" + redirectWorldName + "' not found! Check your config.");
            return;
        }
        player.teleport(redirectWorld.getSpawnLocation());
    }

    /**
     * Starts the register countdown for a player.
     */
    private void startRegisterCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = new BukkitRunnable() {
            int timeLeft = registerTimeSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || !unauthenticatedPlayers.contains(uuid)) {
                    cancel();
                    countdownTasks.remove(uuid);
                    return;
                }

                String subtitle = registerSubtitle.replace("(&#FFFD00ᴘ&#FFFD00ʟ&#FFFD00ᴀ&#FFFD00ᴄ&#FFFD00ᴇ&#FFFD00ʜ&#FFFD00ᴏ&#FFFD00ʟ&#FFFD00ᴅ&#FFFD00ᴇ&#FFFD00ʀ&#FFFD00)", String.valueOf(timeLeft));
                sendTitle(player, registerTitle, subtitle, 10, 20, 10); // Adjust timings

                if (timeLeft <= 0) {
                    player.kickPlayer(prefix + ChatColor.RED + "Time's up! Please register to play.");
                    cancel();
                    countdownTasks.remove(uuid);
                    unauthenticatedPlayers.remove(uuid);
                }
                timeLeft--;
            }
        };
        task.runTaskTimer(this, 0L, 20L); // Run every second (20 ticks)
        countdownTasks.put(uuid, task);
    }

    /**
     * Starts the login countdown for a player.
     */
    private void startLoginCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = new BukkitRunnable() {
            int timeLeft = loginTimeSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || !unauthenticatedPlayers.contains(uuid)) {
                    cancel();
                    countdownTasks.remove(uuid);
                    return;
                }

                String subtitle = loginSubtitle.replace("(&#FFFD00ᴘ&#FFFD00ʟ&#FFFD00ᴀ&#FFFD00ᴄ&#FFFD00ᴇ&#FFFD00ʜ&#FFFD00ᴏ&#FFFD00ʟ&#FFFD00ᴅ&#FFFD00ᴇ&#FFFD00ʀ&#FFFD00)", String.valueOf(timeLeft));
                sendTitle(player, loginTitle, subtitle, 10, 20, 10); // Adjust timings

                if (timeLeft <= 0) {
                    player.kickPlayer(prefix + ChatColor.RED + "Time's up! Please log in to play.");
                    cancel();
                    countdownTasks.remove(uuid);
                    unauthenticatedPlayers.remove(uuid);
                }
                timeLeft--;
            }
        };
        task.runTaskTimer(this, 0L, 20L); // Run every second (20 ticks)
        countdownTasks.put(uuid, task);
    }

    /**
     * Completes authentication for a player: removes from unauthenticated set, cancels tasks, teleports.
     */
    private void completeAuthentication(Player player) {
        UUID uuid = player.getUniqueId();
        unauthenticatedPlayers.remove(uuid);
        if (countdownTasks.containsKey(uuid)) {
            countdownTasks.get(uuid).cancel();
            countdownTasks.remove(uuid);
        }
        sendTitle(player, "", ChatColor.GREEN + "Authentication complete!", 10, 40, 20); // Clear current title and show success
        teleportPlayerToSpawn(player);
    }

    /**
     * Placeholder method for database check.
     * In a real plugin, this would query your database.
     */
    private boolean isPlayerRegistered(UUID uuid) {
        return registeredPlayers.containsKey(uuid); // Replace with real DB check
    }

    // You would add more event handlers here to prevent movement, chat,
    // inventory access, etc., for unauthenticated players.
    // e.g., @EventHandler(ignoreCancelled = true)
    // public void onPlayerMove(PlayerMoveEvent event) {
    //     if (unauthenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
    //         event.setCancelled(true);
    //     }
    // }
}
