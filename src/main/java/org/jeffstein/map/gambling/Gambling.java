package org.jeffstein.map.gambling;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jeffstein.map.gambling.commands.*;
import org.jeffstein.map.gambling.commands.KenoCommand;
import org.jeffstein.map.gambling.commands.LeaderboardCommand;
import org.jeffstein.map.gambling.commands.PokerCommand;
import org.jeffstein.map.gambling.commands.RouletteCommand;
import org.jeffstein.map.gambling.commands.SlotsCommand;
import org.jeffstein.map.gambling.commands.CrapsCommand;
import org.jeffstein.map.gambling.listeners.BaccaratListener;
import org.jeffstein.map.gambling.listeners.BlackjackBettingGUIListener;
import org.jeffstein.map.gambling.listeners.BlackjackGUIListener;
import org.jeffstein.map.gambling.listeners.CasinoLobbyListener;
import org.jeffstein.map.gambling.listeners.CrashListener;
import org.jeffstein.map.gambling.listeners.KenoListener;
import org.jeffstein.map.gambling.listeners.MinesListener;
import org.jeffstein.map.gambling.listeners.PlinkoListener;
import org.jeffstein.map.gambling.listeners.RouletteListener;
import org.jeffstein.map.gambling.listeners.SlotsListener;
import org.jeffstein.map.gambling.listeners.WheelOfFortuneListener;
import org.jeffstein.map.gambling.listeners.CrapsListener;
import org.jeffstein.map.gambling.games.PokerManager;
import org.jeffstein.map.gambling.casino.CasinoBlockManager;
import org.jeffstein.map.gambling.casino.CasinoBlockListener;
import org.jeffstein.map.gambling.casino.PhysicalGameInterface;
import org.jeffstein.map.gambling.casino.CasinoVisualEffects;
import org.jeffstein.map.gambling.commands.CasinoBlockCommand;
import org.jeffstein.map.gambling.config.GamblingConfig;

import java.util.logging.Logger;

public final class Gambling extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private static Leaderboard leaderboard;
    private static Jackpot jackpot;
    private static PokerManager pokerManager;
    private static CasinoBlockManager casinoBlockManager;
    private static PhysicalGameInterface physicalGameInterface;
    private static CasinoVisualEffects visualEffects;
    private static GamblingConfig gamblingConfig;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Initialize configuration first
        gamblingConfig = new GamblingConfig(this);

        leaderboard = new Leaderboard(this);
        jackpot = new Jackpot(this);
        pokerManager = new PokerManager(this);

        // Initialize casino block system (only if enabled in config)
        if (gamblingConfig.isCasinoBlocksEnabled()) {
            casinoBlockManager = new CasinoBlockManager(this);
            physicalGameInterface = new PhysicalGameInterface(this, casinoBlockManager);
            visualEffects = new CasinoVisualEffects(this);

            // Initialize visual effects for existing blocks
            if (gamblingConfig.isHologramsEnabled() || gamblingConfig.isParticlesEnabled()) {
                visualEffects.initializeAllEffects(casinoBlockManager);
            }
        }
        log.info(String.format("[%s] has been enabled!", getName()));
        BlackjackCommand blackjackCommand = new BlackjackCommand(this);
        getCommand("blackjack").setExecutor(blackjackCommand);
        getCommand("blackjack").setTabCompleter(new BlackjackTabCompleter(blackjackCommand.getGames()));
        getCommand("coinflip").setExecutor(new CoinflipCommand());
        getCommand("slots").setExecutor(new SlotsCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand());
        getCommand("jackpot").setExecutor(new JackpotCommand());
        getCommand("baccarat").setExecutor(new BaccaratCommand(this));
        getCommand("roulette").setExecutor(new RouletteCommand(this));
        getCommand("blackjackgui").setExecutor(new BlackjackGUICommand(this));
        getCommand("keno").setExecutor(new KenoCommand(this));
        getCommand("craps").setExecutor(new CrapsCommand(this));
        getCommand("crash").setExecutor(new CrashCommand(this));
        getCommand("plinko").setExecutor(new PlinkoCommand(this));
        getCommand("wheel").setExecutor(new WheelOfFortuneCommand(this));
        getCommand("mines").setExecutor(new MinesCommand(this));
        getCommand("poker").setExecutor(new PokerCommand(this));
        getCommand("casino").setExecutor(new CasinoCommand(this));

        // Register casino block command only if system is enabled
        if (gamblingConfig.isCasinoBlocksEnabled()) {
            getCommand("casinoblock").setExecutor(new CasinoBlockCommand(this, casinoBlockManager, physicalGameInterface));
        }
        getServer().getPluginManager().registerEvents(new SlotsListener(this), this);
        getServer().getPluginManager().registerEvents(new BaccaratListener(this), this);
        getServer().getPluginManager().registerEvents(new RouletteListener(this), this);

        // Register blackjack listeners with shared games map
        BlackjackBettingGUIListener blackjackBettingListener = new BlackjackBettingGUIListener(this);
        getServer().getPluginManager().registerEvents(blackjackBettingListener, this);
        getServer().getPluginManager().registerEvents(new BlackjackGUIListener(this, blackjackBettingListener.getGames()), this);

        getServer().getPluginManager().registerEvents(new KenoListener(this), this);
        getServer().getPluginManager().registerEvents(new CrapsListener(this), this);
        getServer().getPluginManager().registerEvents(new CrashListener(this), this);
        getServer().getPluginManager().registerEvents(new PlinkoListener(this), this);
        getServer().getPluginManager().registerEvents(new WheelOfFortuneListener(this), this);
        getServer().getPluginManager().registerEvents(new MinesListener(this), this);
        getServer().getPluginManager().registerEvents(new CasinoLobbyListener(this), this);

        // Register casino block listeners only if system is enabled
        if (gamblingConfig.isCasinoBlocksEnabled()) {
            getServer().getPluginManager().registerEvents(new CasinoBlockListener(this, casinoBlockManager), this);
            if (gamblingConfig.isPhysicalInteractionsEnabled()) {
                getServer().getPluginManager().registerEvents(physicalGameInterface, this);
            }
        }
    }

    @Override
    public void onDisable() {
        // Shutdown casino block system
        if (casinoBlockManager != null) {
            casinoBlockManager.shutdown();
        }
        if (physicalGameInterface != null) {
            physicalGameInterface.clearAllInteractions();
        }
        if (visualEffects != null) {
            visualEffects.cleanup();
        }

        log.info(String.format("[%s] has been disabled!", getName()));
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

    public static PokerManager getPokerManager() {
        return pokerManager;
    }

    public static CasinoBlockManager getCasinoBlockManager() {
        return casinoBlockManager;
    }

    public static PhysicalGameInterface getPhysicalGameInterface() {
        return physicalGameInterface;
    }

    public static CasinoVisualEffects getVisualEffects() {
        return visualEffects;
    }

    public static GamblingConfig getGamblingConfig() {
        return gamblingConfig;
    }
}