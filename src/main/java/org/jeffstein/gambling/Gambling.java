package org.jeffstein.gambling;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jeffstein.gambling.commands.*;
import org.jeffstein.gambling.commands.KenoCommand;
import org.jeffstein.gambling.commands.LeaderboardCommand;
import org.jeffstein.gambling.commands.PokerCommand;
import org.jeffstein.gambling.commands.RouletteCommand;
import org.jeffstein.gambling.commands.SlotsCommand;
import org.jeffstein.gambling.commands.CrapsCommand;
import org.jeffstein.gambling.listeners.BaccaratListener;
import org.jeffstein.gambling.listeners.BlackjackBettingGUIListener;
import org.jeffstein.gambling.listeners.BlackjackGUIListener;
import org.jeffstein.gambling.listeners.CrashListener;
import org.jeffstein.gambling.listeners.KenoListener;
import org.jeffstein.gambling.listeners.PlinkoListener;
import org.jeffstein.gambling.listeners.RouletteListener;
import org.jeffstein.gambling.listeners.SlotsListener;
import org.jeffstein.gambling.listeners.WheelOfFortuneListener;
import org.jeffstein.gambling.listeners.CrapsListener;

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
        BlackjackCommand blackjackCommand = new BlackjackCommand(this);
        getCommand("blackjack").setExecutor(blackjackCommand);
        getCommand("blackjack").setTabCompleter(new BlackjackTabCompleter(blackjackCommand.getGames()));
        getCommand("coinflip").setExecutor(new CoinflipCommand());
        getCommand("slots").setExecutor(new SlotsCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand());
        getCommand("jackpot").setExecutor(new JackpotCommand());
        getCommand("poker").setExecutor(new PokerCommand(this));
        getCommand("baccarat").setExecutor(new BaccaratCommand(this));
        getCommand("roulette").setExecutor(new RouletteCommand(this));
        getCommand("blackjackgui").setExecutor(new BlackjackGUICommand(this));
        getCommand("keno").setExecutor(new KenoCommand(this));
        getCommand("craps").setExecutor(new CrapsCommand(this));
        getCommand("crash").setExecutor(new CrashCommand(this));
        getCommand("plinko").setExecutor(new PlinkoCommand(this));
        getCommand("wheel").setExecutor(new WheelOfFortuneCommand(this));
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