package org.jeffstein.gambling.listeners;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BaccaratGUI;
import org.jeffstein.gambling.games.BaccaratGame;
import org.jeffstein.gambling.games.Card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaccaratListener implements Listener {

    private static BaccaratListener instance;

    public static BaccaratListener getInstance() {
        return instance;
    }

    private final Gambling plugin;
    private final Map<UUID, String> betSelections = new HashMap<>();
    private final Map<UUID, Double> betAmounts = new HashMap<>();
    private final Map<UUID, ActiveRound> activeRounds = new HashMap<>();
    private final Map<UUID, String[]> lastSummaries = new HashMap<>();

    public BaccaratListener(Gambling plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BaccaratGUI)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        BaccaratGUI gui = (BaccaratGUI) holder;

        int slot = event.getRawSlot();

        if (slot == 11) {
            betSelections.put(playerId, "player");
            gui.setSelectedBet("player");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.sendMessage(ChatColor.BLUE + "[BACCARAT] Betting on PLAYER (2:1)");
        } else if (slot == 13) {
            betSelections.put(playerId, "tie");
            gui.setSelectedBet("tie");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.sendMessage(ChatColor.WHITE + "[BACCARAT] Betting on TIE (9:1)");
        } else if (slot == 15) {
            betSelections.put(playerId, "banker");
            gui.setSelectedBet("banker");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.sendMessage(ChatColor.RED + "[BACCARAT] Betting on BANKER (1.95:1)");
        } else if (slot >= 19 && slot <= 21) {
            double amount = slot == 19 ? 100 : slot == 20 ? 500 : 1000;
            betAmounts.put(playerId, amount);
            gui.setSelectedAmount(amount);

            Economy economy = Gambling.getEconomy();
            if (economy.getBalance(player) < amount) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] Need " + economy.format(amount) + " to place that bet.");
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.sendMessage(ChatColor.GOLD + "[BACCARAT] Bet set to " + economy.format(amount));
            }
        } else if (slot == 22) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                if (name.equalsIgnoreCase("ROUND ACTIVE")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[BACCARAT] Please wait for the current round to finish.");
                    return;
                }
                if (name.equalsIgnoreCase("NEW ROUND")) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    resetSelections(playerId, gui);
                    return;
                }
            }

            if (activeRounds.containsKey(playerId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] Round already in progress.");
                return;
            }

            if (!betSelections.containsKey(playerId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] Choose Player, Banker, or Tie first.");
                return;
            }

            if (!betAmounts.containsKey(playerId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] Pick a bet amount first.");
                return;
            }

            double wager = betAmounts.get(playerId);
            Economy economy = Gambling.getEconomy();

            if (economy.getBalance(player) < wager) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] You need " + economy.format(wager) + ".");
                return;
            }

            economy.withdrawPlayer(player, wager);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(ChatColor.YELLOW + "[BACCARAT] Bet " + economy.format(wager) + " on " + betSelections.get(playerId).toUpperCase());

            startAnimatedRound(player, gui, betSelections.get(playerId), wager);
        } else if (slot == 25) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.sendMessage(ChatColor.AQUA + "[BACCARAT] HOW TO PLAY:");
            player.sendMessage(ChatColor.WHITE + "• Choose Player, Banker, or Tie");
            player.sendMessage(ChatColor.WHITE + "• Select your wager amount");
            player.sendMessage(ChatColor.WHITE + "• Cards follow standard third-card rules");
            player.sendMessage(ChatColor.YELLOW + "Aces = 1, 10s/Face cards = 0");
        } else if (slot == 26) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            player.sendMessage(ChatColor.GRAY + "[BACCARAT] Thanks for visiting the table.");
        }
    }

    private void startAnimatedRound(Player player, BaccaratGUI gui, String betType, double wager) {
        UUID playerId = player.getUniqueId();

        ActiveRound round = new ActiveRound(player, gui, betType, wager);
        activeRounds.put(playerId, round);
        round.begin();
    }

    private void resetSelections(UUID playerId, BaccaratGUI gui) {
        betSelections.remove(playerId);
        betAmounts.remove(playerId);
        gui.resetForNewGame();

        if (lastSummaries.containsKey(playerId)) {
            gui.setRoundSummary("Last Result", lastSummaries.get(playerId));
        }
    }

    private class ActiveRound {
        private final Player player;
        private final UUID playerId;
        private final BaccaratGUI gui;
        private final BaccaratGame game = new BaccaratGame();
        private final String betType;
        private final double wager;

        private BukkitRunnable task;
        private String playerAction = ChatColor.BLUE + "Player awaiting action";
        private String bankerAction = ChatColor.RED + "Banker awaiting action";

        ActiveRound(Player player, BaccaratGUI gui, String betType, double wager) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.gui = gui;
            this.betType = betType;
            this.wager = wager;
        }

        void begin() {
            game.beginRound();
            gui.resetForNewGame();
            gui.setSelectedBet(betType);
            if (wager == 100 || wager == 500 || wager == 1000) {
                gui.setSelectedAmount(wager);
            }
            gui.setStatus("Shuffling Deck",
                ChatColor.GRAY + "Good luck, " + player.getName() + "!",
                ChatColor.YELLOW + "Round in progress...");
            gui.setActionButton(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "ROUND ACTIVE",
                ChatColor.GRAY + "Please wait for the reveal");

            if (lastSummaries.containsKey(playerId)) {
                gui.setRoundSummary("Last Result", lastSummaries.get(playerId));
            }

            task = new BukkitRunnable() {
                private int step = 0;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cleanup();
                        cancel();
                        return;
                    }

                    switch (step) {
                        case 0:
                            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.0f);
                            gui.updateHands(game.getPlayerHand(), game.getBankerHand());
                            break;
                        case 1:
                            drawCardToPlayer("First card to Player");
                            break;
                        case 2:
                            drawCardToBanker("First card to Banker");
                            break;
                        case 3:
                            drawCardToPlayer("Second card to Player");
                            break;
                        case 4:
                            drawCardToBanker("Second card to Banker");
                            game.evaluateNaturals();
                            if (game.isNaturalRound()) {
                                finishRound(game.determineWinner(), naturalSummary());
                                cancel();
                                return;
                            }
                            break;
                        case 5:
                            handlePlayerThirdCard();
                            break;
                        case 6:
                            handleBankerThirdCard();
                            break;
                        case 7:
                            finishRound(game.determineWinner(), finalSummary());
                            cancel();
                            return;
                        default:
                            cancel();
                            return;
                    }

                    step++;
                }
            };

            task.runTaskTimer(plugin, 0L, 12L);
        }

        private void drawCardToPlayer(String message) {
            Card card = game.dealPlayerCard();
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 1.2f);
            gui.updateHands(game.getPlayerHand(), game.getBankerHand());
            player.sendMessage(ChatColor.BLUE + "[BACCARAT] " + message + ": " + card);
        }

        private void drawCardToBanker(String message) {
            Card card = game.dealBankerCard();
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 0.8f);
            gui.updateHands(game.getPlayerHand(), game.getBankerHand());
            player.sendMessage(ChatColor.RED + "[BACCARAT] " + message + ": " + card);
        }

        private void handlePlayerThirdCard() {
            if (game.shouldPlayerDrawThirdCard()) {
                Card card = game.drawPlayerThirdCard();
                gui.updateHands(game.getPlayerHand(), game.getBankerHand());
                playerAction = ChatColor.BLUE + "Player draws " + card.getRank() + " (total " + game.getPlayerValue() + ")";
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
                player.sendMessage(ChatColor.BLUE + "[BACCARAT] Player draws third card: " + card);
            } else {
                playerAction = ChatColor.BLUE + "Player stands on " + game.getPlayerValue();
                player.sendMessage(ChatColor.BLUE + "[BACCARAT] Player stands on " + game.getPlayerValue());
            }
        }

        private void handleBankerThirdCard() {
            Integer playerThirdValue = game.playerDrewThirdCard() ? game.getBaccaratValue(game.getPlayerThirdCard()) : null;

            if (game.shouldBankerDrawThirdCard(playerThirdValue)) {
                Card card = game.drawBankerThirdCard();
                gui.updateHands(game.getPlayerHand(), game.getBankerHand());
                bankerAction = ChatColor.RED + "Banker draws " + card.getRank() + " (total " + game.getBankerValue() + ")";
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 0.8f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] Banker draws third card: " + card);
            } else {
                bankerAction = ChatColor.RED + "Banker stands on " + game.getBankerValue();
                player.sendMessage(ChatColor.RED + "[BACCARAT] Banker stands on " + game.getBankerValue());
            }
        }

        private String naturalSummary() {
            playerAction = ChatColor.BLUE + "Player natural " + game.getPlayerValue();
            bankerAction = ChatColor.RED + "Banker natural " + game.getBankerValue();
            return ChatColor.YELLOW + "Natural " + Math.max(game.getPlayerValue(), game.getBankerValue()) + " decides it.";
        }

        private String finalSummary() {
            return ChatColor.YELLOW + "Player " + game.getPlayerValue() + " vs Banker " + game.getBankerValue();
        }

        private void finishRound(BaccaratGame.Winner winner, String summaryLine) {
            gui.updateHands(game.getPlayerHand(), game.getBankerHand());
            gui.showOutcome(winner, summaryLine);

            String[] summary = new String[] {
                ChatColor.BLUE + "Player: " + game.getPlayerValue(),
                playerAction,
                ChatColor.RED + "Banker: " + game.getBankerValue(),
                bankerAction
            };
            lastSummaries.put(playerId, summary);
            gui.setRoundSummary("Last Result", summary);

            distributePayout(winner);
            cleanup();
        }

        private void distributePayout(BaccaratGame.Winner winner) {
            Economy economy = Gambling.getEconomy();
            boolean playerWon = false;
            boolean push = false;
            double payout = 0.0;

            switch (betType) {
                case "player":
                    if (winner == BaccaratGame.Winner.PLAYER) {
                        payout = wager * 2;
                        playerWon = true;
                    } else if (winner == BaccaratGame.Winner.TIE) {
                        push = true;
                    }
                    break;
                case "banker":
                    if (winner == BaccaratGame.Winner.BANKER) {
                        payout = wager * 1.95;
                        playerWon = true;
                    } else if (winner == BaccaratGame.Winner.TIE) {
                        push = true;
                    }
                    break;
                case "tie":
                    if (winner == BaccaratGame.Winner.TIE) {
                        payout = wager * 9;
                        playerWon = true;
                    }
                    break;
            }

            if (playerWon) {
                economy.depositPlayer(player, payout);
                Gambling.getLeaderboard().addWin(playerId, wager);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.3f);
                player.sendTitle(ChatColor.GREEN + "YOU WON!", ChatColor.GOLD + "+" + economy.format(payout), 10, 50, 20);
            } else if (push) {
                economy.depositPlayer(player, wager);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.sendTitle(ChatColor.YELLOW + "PUSH", ChatColor.GRAY + "Bet returned", 10, 50, 20);
            } else {
                Gambling.getLeaderboard().addLoss(playerId, wager);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendTitle(ChatColor.RED + "YOU LOST", ChatColor.GRAY + "-" + economy.format(wager), 10, 50, 20);
            }

            betAmounts.remove(playerId);
            betSelections.remove(playerId);
        }

        private void cleanup() {
            if (task != null) {
                task.cancel();
            }
            activeRounds.remove(playerId);
        }
    }

    public void startWithBet(Player player, String betType, double amount) {
        UUID playerId = player.getUniqueId();
        String normalized = betType.toLowerCase();

        if (!normalized.equals("player") && !normalized.equals("banker") && !normalized.equals("tie")) {
            player.sendMessage(ChatColor.RED + "[BACCARAT] Invalid bet type. Use player, banker, or tie.");
            return;
        }

        Economy economy = Gambling.getEconomy();
        if (economy.getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "[BACCARAT] Not enough balance for that bet.");
            return;
        }

        BaccaratGUI gui = new BaccaratGUI(plugin, player);
        gui.openInventory();
        gui.setSelectedBet(normalized);
        if (amount == 100 || amount == 500 || amount == 1000) {
            gui.setSelectedAmount(amount);
        }

        betSelections.put(playerId, normalized);
        betAmounts.put(playerId, amount);

        Economy econ = Gambling.getEconomy();
        econ.withdrawPlayer(player, amount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.YELLOW + "[BACCARAT] Bet " + econ.format(amount) + " on " + normalized.toUpperCase());

        startAnimatedRound(player, gui, normalized, amount);
    }

    public void playCommand(Player player, String betType, double amount) {
        String normalized = betType.toLowerCase();
        if (!normalized.equals("player") && !normalized.equals("banker") && !normalized.equals("tie")) {
            player.sendMessage(ChatColor.RED + "[BACCARAT] Invalid bet type. Use player, banker, or tie.");
            return;
        }

        Economy economy = Gambling.getEconomy();
        if (economy.getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "[BACCARAT] Not enough balance for that bet.");
            return;
        }

        economy.withdrawPlayer(player, amount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.YELLOW + "[BACCARAT] Bet " + economy.format(amount) + " on " + normalized.toUpperCase());

        BaccaratGame game = new BaccaratGame();
        game.beginRound();

        runCommandAnimation(player, normalized, amount, economy, game);
    }

    private String getHandString(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            sb.append(card.getRank()).append(" of ").append(card.getSuit());
            if (i < hand.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private void runCommandAnimation(Player player, String betType, double amount, Economy economy, BaccaratGame game) {
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                switch (step) {
                    case 0:
                        game.dealPlayerCard();
                        player.sendMessage(ChatColor.BLUE + "[BACCARAT] Player draws " + game.getPlayerHand().get(game.getPlayerHand().size() - 1));
                        break;
                    case 1:
                        game.dealBankerCard();
                        player.sendMessage(ChatColor.RED + "[BACCARAT] Banker draws " + game.getBankerHand().get(game.getBankerHand().size() - 1));
                        break;
                    case 2:
                        game.dealPlayerCard();
                        player.sendMessage(ChatColor.BLUE + "[BACCARAT] Player draws " + game.getPlayerHand().get(game.getPlayerHand().size() - 1));
                        break;
                    case 3:
                        game.dealBankerCard();
                        player.sendMessage(ChatColor.RED + "[BACCARAT] Banker draws " + game.getBankerHand().get(game.getBankerHand().size() - 1));
                        game.evaluateNaturals();
                        if (game.isNaturalRound()) {
                            concludeCommandRound(player, betType, amount, economy, game);
                            this.cancel();
                        }
                        break;
                    case 4:
                        if (game.shouldPlayerDrawThirdCard()) {
                            Card third = game.drawPlayerThirdCard();
                            player.sendMessage(ChatColor.BLUE + "[BACCARAT] Player draws third card " + third);
                        }
                        break;
                    case 5:
                        Integer playerThird = game.playerDrewThirdCard() ? game.getBaccaratValue(game.getPlayerThirdCard()) : null;
                        if (game.shouldBankerDrawThirdCard(playerThird)) {
                            Card third = game.drawBankerThirdCard();
                            player.sendMessage(ChatColor.RED + "[BACCARAT] Banker draws third card " + third);
                        }
                        concludeCommandRound(player, betType, amount, economy, game);
                        this.cancel();
                        break;
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void concludeCommandRound(Player player, String betType, double amount, Economy economy, BaccaratGame game) {
        BaccaratGame.Winner winner = game.determineWinner();
        int playerValue = game.getPlayerValue();
        int bankerValue = game.getBankerValue();

        player.sendMessage(ChatColor.BLUE + "[BACCARAT] Player hand (" + playerValue + "): " + getHandString(game.getPlayerHand()));
        player.sendMessage(ChatColor.RED + "[BACCARAT] Banker hand (" + bankerValue + "): " + getHandString(game.getBankerHand()));

        switch (winner) {
            case PLAYER -> player.sendMessage(ChatColor.YELLOW + "[BACCARAT] " + ChatColor.BLUE + "Player wins" + ChatColor.YELLOW + " (" + playerValue + " vs " + bankerValue + ")");
            case BANKER -> player.sendMessage(ChatColor.YELLOW + "[BACCARAT] " + ChatColor.RED + "Banker wins" + ChatColor.YELLOW + " (" + playerValue + " vs " + bankerValue + ")");
            default -> player.sendMessage(ChatColor.YELLOW + "[BACCARAT] " + ChatColor.WHITE + "It's a tie" + ChatColor.YELLOW + " (" + playerValue + " vs " + bankerValue + ")");
        }

        double payout = 0;
        if (betType.equals(winner.name().toLowerCase())) {
            if (winner == BaccaratGame.Winner.PLAYER) {
                payout = amount * 2;
            } else if (winner == BaccaratGame.Winner.BANKER) {
                payout = amount * 1.95;
            } else {
                payout = amount * 9;
            }
            economy.depositPlayer(player, payout);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), amount);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "[BACCARAT] You won " + economy.format(payout) + "!");
        } else {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), amount);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[BACCARAT] Better luck next time.");
        }
    }
}
