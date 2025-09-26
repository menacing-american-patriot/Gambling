package org.jeffstein.gambling;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jeffstein.gambling.commands.*;
import org.jeffstein.gambling.listeners.SlotsListener;

import java.util.logging.Logger;

public final class Gambling extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private static Leaderboard leaderboard;
    private static Jackpot jackpot;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        leaderboard = new Leaderboard(this);
        jackpot = new Jackpot(this);
        log.info(String.format("[%s] has been enabled!", getDescription().getName()));
        getCommand("coinflip").setExecutor(new CoinflipCommand());
        getCommand("slots").setExecutor(new SlotsCommand(this));
        getCommand("blackjack").setExecutor(new BlackjackCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand());
        getCommand("jackpot").setExecutor(new JackpotCommand());
        getServer().getPluginManager().registerEvents(new SlotsListener(this), this);
    }

    @Override
    public void onDisable() {
        log.info(String.format("[%s] has been disabled!", getDescription().getName()));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public static Jackpot getJackpot() {
        return jackpot;
    }
}