package org.jeffstein.map.gambling.listeners;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.CrapsGUI;
import org.jeffstein.map.gambling.games.CrapsGame;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CrapsListener implements Listener {

    private static CrapsListener instance;

    public static CrapsListener getInstance() {
        return instance;
    }

    private final Gambling plugin;
    private final Map<UUID, CrapsTable> tables = new HashMap<>();

    public CrapsListener(Gambling plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CrapsGUI)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));

        int slot = event.getRawSlot();

        switch (slot) {
            case 37:
                table.gui.adjustBet(-1000);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                break;
            case 38:
                table.gui.adjustBet(-100);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                break;
            case 42:
                table.gui.adjustBet(100);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                break;
            case 43:
                table.gui.adjustBet(1000);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                break;
            case 30:
                handleBet(table, player, true);
                break;
            case 32:
                handleBet(table, player, false);
                break;
            case 44:
                if (!table.rolling) {
                    if (!table.hasActiveBets()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        table.gui.setSummary("No Bets", ChatColor.RED + "Place a bet before rolling.");
                        break;
                    }
                    startRollAnimation(table);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                break;
            case 45:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GRAY + "[CRAPS] Thanks for stopping by the table!");
                break;
            default:
                break;
        }

        table.refreshUi();
    }

    private void handleBet(CrapsTable table, Player player, boolean passLine) {
        if (table.rolling) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        double amount = table.gui.getCurrentBet();
        Economy economy = Gambling.getEconomy();

        if (economy.getBalance(player) < amount) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRAPS] You need " + economy.format(amount) + " to place that bet.");
            return;
        }

        economy.withdrawPlayer(player, amount);
        if (passLine) {
            table.game.addPassLineBet(amount);
            player.sendMessage(ChatColor.GREEN + "[CRAPS] Pass Line bet placed: " + economy.format(amount));
        } else {
            table.game.addDontPassBet(amount);
            player.sendMessage(ChatColor.RED + "[CRAPS] Don't Pass bet placed: " + economy.format(amount));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        table.gui.setSummary("Bet Placed",
            ChatColor.GRAY + "Pass: " + economy.format(table.game.getPassLineBet()),
            ChatColor.GRAY + "Don't: " + economy.format(table.game.getDontPassBet()));
    }

    private void startRollAnimation(CrapsTable table) {
        table.rolling = true;
        table.gui.setActionReady(false, table.hasActiveBets());
        table.gui.setSummary("Rolling",
            ChatColor.YELLOW + "Dice are tumbling...",
            ChatColor.GRAY + "Good luck!");
        Player player = table.player;

        new BukkitRunnable() {
            private int frames = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    table.rolling = false;
                    return;
                }

                if (frames >= 6) {
                    cancel();
                    concludeRoll(table);
                    return;
                }

                int mockDieOne = ThreadLocalRandom.current().nextInt(1, 7);
                int mockDieTwo = ThreadLocalRandom.current().nextInt(1, 7);
                table.gui.showRollingFrame(mockDieOne, mockDieTwo);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                frames++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void concludeRoll(CrapsTable table) {
        int[] dice = table.game.rollDice();
        finalizeRoll(table, dice[0], dice[1], true);
    }

    private void finalizeRoll(CrapsTable table, int dieOne, int dieTwo, boolean showTitles) {
        Player player = table.player;
        int total = dieOne + dieTwo;

        table.gui.setDiceDisplay(dieOne, dieTwo);

        CrapsGame.RollOutcome outcome = table.game.resolveRoll(total);
        table.gui.setPointDisplay(table.game.getState(), table.game.getPoint());
        table.gui.setHistory(table.game.getRecentTotals());

        player.sendMessage(ChatColor.YELLOW + "[CRAPS] Dice rolled: " + dieOne + " + " + dieTwo + " = " + total);

        double passBet = table.game.getPassLineBet();
        double dontBet = table.game.getDontPassBet();

        Economy economy = Gambling.getEconomy();
        double profit = 0;

        if (outcome.passLineWins() && passBet > 0) {
            economy.depositPlayer(player, passBet * 2);
            profit += passBet;
            Gambling.getLeaderboard().addWin(player.getUniqueId(), passBet);
            table.game.clearPassLineBet();
        } else if (outcome.passLineLoses() && passBet > 0) {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), passBet);
            table.game.clearPassLineBet();
        }

        if (outcome.dontPassWins() && dontBet > 0) {
            economy.depositPlayer(player, dontBet * 2);
            profit += dontBet;
            Gambling.getLeaderboard().addWin(player.getUniqueId(), dontBet);
            table.game.clearDontPassBet();
        } else if (outcome.dontPassPush() && dontBet > 0) {
            economy.depositPlayer(player, dontBet);
            table.game.clearDontPassBet();
        } else if (outcome.dontPassLoses() && dontBet > 0) {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), dontBet);
            table.game.clearDontPassBet();
        }

        table.gui.highlightBets(table.game.getPassLineBet(), table.game.getDontPassBet());

        String summary = ChatColor.YELLOW + "Rolled " + dieOne + " + " + dieTwo + " = " + total;
        table.gui.setSummary("Roll Result",
            summary,
            ChatColor.WHITE + outcome.message());

        if (profit > 0) {
            if (showTitles) {
                player.sendTitle(ChatColor.GREEN + "WIN", ChatColor.GOLD + "+" + economy.format(profit), 10, 40, 10);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "[CRAPS] " + outcome.message() + " You won " + economy.format(profit) + ".");
        } else if (outcome.dontPassPush()) {
            if (showTitles) {
                player.sendTitle(ChatColor.YELLOW + "PUSH", ChatColor.GRAY + "Don't Pass returned", 10, 40, 10);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.sendMessage(ChatColor.YELLOW + "[CRAPS] " + outcome.message());
        } else if (outcome.passLineLoses() || outcome.dontPassLoses()) {
            if (showTitles) {
                player.sendTitle(ChatColor.RED + "LOSS", ChatColor.GRAY + outcome.message(), 10, 40, 10);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRAPS] " + outcome.message());
        } else {
            player.sendMessage(ChatColor.GRAY + "[CRAPS] " + outcome.message());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.3f);
        }

        table.rolling = false;
        table.refreshUi();
        table.gui.setStatus("ROLL RESULT",
            ChatColor.WHITE + outcome.message());
    }

    public void openTable(Player player) {
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));
        table.gui.openInventory();
        table.refreshUi();
    }

    public void startWithBet(Player player, String betType, double amount, boolean rollNow) {
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));

        boolean passLine;
        if (betType.equalsIgnoreCase("pass")) {
            passLine = true;
        } else if (betType.equalsIgnoreCase("dontpass") || betType.equalsIgnoreCase("don'tpass")) {
            passLine = false;
        } else {
            player.sendMessage(ChatColor.RED + "[CRAPS] Invalid bet type. Use pass or dontpass.");
            return;
        }

        Economy economy = Gambling.getEconomy();
        if (economy.getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "[CRAPS] Not enough balance for that bet.");
            return;
        }

        economy.withdrawPlayer(player, amount);
        if (passLine) {
            table.game.addPassLineBet(amount);
            player.sendMessage(ChatColor.GREEN + "[CRAPS] Pass Line bet placed: " + economy.format(amount));
        } else {
            table.game.addDontPassBet(amount);
            player.sendMessage(ChatColor.RED + "[CRAPS] Don't Pass bet placed: " + economy.format(amount));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        table.gui.openInventory();
        table.refreshUi();

        if (rollNow && !table.rolling) {
            startRollAnimation(table);
        }
    }

    public void placeBetCommand(Player player, double amount, boolean passLine) {
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));
        handleBet(table, player, passLine);
    }

    public void rollCommand(Player player) {
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));

        if (table.rolling) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRAPS] Dice are already rolling.");
            return;
        }

        if (!table.hasActiveBets()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRAPS] Place a Pass or Don't Pass bet first.");
            return;
        }

        table.rolling = true;
        int[] dice = table.game.rollDice();
        finalizeRoll(table, dice[0], dice[1], false);
    }

    public void statusCommand(Player player) {
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));

        CrapsGame.GameState state = table.game.getState();
        int point = table.game.getPoint();
        double pass = table.game.getPassLineBet();
        double dont = table.game.getDontPassBet();

        player.sendMessage(ChatColor.YELLOW + "[CRAPS] Current state: " + (state == CrapsGame.GameState.COME_OUT ? "Come Out" : "Point"));
        if (state == CrapsGame.GameState.POINT) {
            player.sendMessage(ChatColor.YELLOW + "[CRAPS] Point: " + point);
        }
        player.sendMessage(ChatColor.GREEN + "[CRAPS] Pass Line bet: " + Gambling.getEconomy().format(pass));
        player.sendMessage(ChatColor.RED + "[CRAPS] Don't Pass bet: " + Gambling.getEconomy().format(dont));

        if (!table.game.getRecentTotals().isEmpty()) {
            StringBuilder history = new StringBuilder();
            for (int i = 0; i < table.game.getRecentTotals().size(); i++) {
                if (i > 0) {
                    history.append(ChatColor.GRAY).append(", ");
                }
                history.append(ChatColor.YELLOW).append(table.game.getRecentTotals().get(i));
            }
            player.sendMessage(ChatColor.YELLOW + "[CRAPS] Recent rolls: " + history);
        } else {
            player.sendMessage(ChatColor.GRAY + "[CRAPS] No rolls yet.");
        }
    }

    public void resetCommand(Player player) {
        CrapsTable table = tables.computeIfAbsent(player.getUniqueId(), id -> new CrapsTable(player));
        table.game.resetForNewRound();
        table.refreshUi();
        player.sendMessage(ChatColor.GRAY + "[CRAPS] Table reset. New come-out roll.");
    }

    private class CrapsTable {
        private final Player player;
        private final CrapsGUI gui;
        private final CrapsGame game = new CrapsGame();
        private boolean rolling = false;

        CrapsTable(Player player) {
            this.player = player;
            this.gui = new CrapsGUI(player);
            refreshUi();
        }

        boolean hasActiveBets() {
            return game.getPassLineBet() > 0 || game.getDontPassBet() > 0;
        }

        void refreshUi() {
            gui.highlightBets(game.getPassLineBet(), game.getDontPassBet());
            gui.setPointDisplay(game.getState(), game.getPoint());
            gui.setActionReady(!rolling, hasActiveBets());
            gui.setHistory(game.getRecentTotals());
            if (game.getState() == CrapsGame.GameState.COME_OUT) {
                gui.setStatus("COME OUT ROLL",
                    ChatColor.GRAY + "7/11 wins Pass, 2/3/12 loses",
                    ChatColor.YELLOW + "Establish a point to continue");
            } else {
                gui.setStatus("POINT PHASE",
                    ChatColor.GRAY + "Pass needs " + game.getPoint() + " before a 7",
                    ChatColor.YELLOW + "Don't Pass wins on 7 out");
            }
        }
    }
}
