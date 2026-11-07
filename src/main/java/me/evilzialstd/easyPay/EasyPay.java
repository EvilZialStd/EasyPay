package me.evilzialstd.easyPay;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EasyPay extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Integer> balances = new HashMap<>();
    private int startingBalance;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        startingBalance = getConfig().getInt("starting-balance");

        if (startingBalance < 0) {
            getLogger().severe("starting-balance can`t be negative.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("balance").setExecutor(this);
        getCommand("pay").setExecutor(this);
        getLogger().info("enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!balances.containsKey(id)) {
            balances.put(id, startingBalance);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Is the sender a player?
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("players-only"));
            return true;
        }

        // UUID player
        UUID uuid = player.getUniqueId();
        int bal = balances.getOrDefault(uuid, 0);

        // /balance
        if (cmd.getName().equalsIgnoreCase("balance")) {
            player.sendMessage(message("balance", "balance", bal));
            return true;
        }

        // Check format
        if (args.length < 2) {
            player.sendMessage(message("pay-usage"));
            return true;
        }


        // Target player
        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(message("player-offline"));
            return true;
        }


        // Amount to send
        int transfer;
        try {
            transfer = Integer.parseInt(args[1]);
        } catch (NumberFormatException e){
            player.sendMessage(message("invalid-amount"));
            return true;
        }

        UUID uuidTarget = target.getUniqueId();
        int balTarget = balances.getOrDefault(uuidTarget, 0);
        // /pay
        if (cmd.getName().equalsIgnoreCase("pay")) {
            if (bal >= transfer) {
                balances.put(uuid, bal - transfer);
                player.sendMessage(message("sent", "player", target.getName(), "amount", transfer));

                balances.put(uuidTarget, balTarget + transfer);
                target.sendMessage(message("received", "player", player.getName(), "amount", transfer));
            } else {
                player.sendMessage(message("insufficient-funds", "balance", bal));
            }
        }


        return true;
    }

    private String message(String key, Object... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Placeholders and values must be provided in pairs"
            );
        }

        String text = getConfig().getString("messages." + key, key);

        for (int i = 0; i < replacements.length; i += 2) {
            text = text.replace(
                    "{" + replacements[i] + "}",
                    String.valueOf(replacements[i + 1])
            );
        }

        return text;
    }
}
