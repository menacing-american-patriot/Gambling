package org.jeffstein.map.gambling.casino;

import org.bukkit.Location;

/**
 * Represents a physical interaction point around a casino block
 */
public class PhysicalInteraction {
    
    private final InteractionType type;
    private final Location casinoBlockLocation;
    private final String displayName;
    private final Object data; // Additional data for the interaction
    
    public PhysicalInteraction(InteractionType type, Location casinoBlockLocation, String displayName) {
        this(type, casinoBlockLocation, displayName, null);
    }
    
    public PhysicalInteraction(InteractionType type, Location casinoBlockLocation, String displayName, Object data) {
        this.type = type;
        this.casinoBlockLocation = casinoBlockLocation.clone();
        this.displayName = displayName;
        this.data = data;
    }
    
    public InteractionType getType() {
        return type;
    }
    
    public Location getCasinoBlockLocation() {
        return casinoBlockLocation.clone();
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Object getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return String.format("PhysicalInteraction{type=%s, displayName='%s', casinoBlock=%s}", 
                           type, displayName, casinoBlockLocation);
    }
}

/**
 * Types of physical interactions available around casino blocks
 */
enum InteractionType {
    // Blackjack interactions
    BLACKJACK_HIT,
    BLACKJACK_STAND,
    BLACKJACK_DOUBLE,
    BLACKJACK_SPLIT,
    
    // Roulette interactions
    ROULETTE_SPIN,
    ROULETTE_BET,
    ROULETTE_CLEAR_BETS,
    
    // Craps interactions
    CRAPS_ROLL,
    CRAPS_PASS_BET,
    CRAPS_DONT_PASS_BET,
    CRAPS_FIELD_BET,
    CRAPS_ODDS_BET,
    
    // Crash interactions
    CRASH_CASHOUT,
    CRASH_INCREASE_BET,
    CRASH_DECREASE_BET,
    CRASH_AUTO_CASHOUT,
    
    // Plinko interactions
    PLINKO_DROP,
    PLINKO_ADJUST_BET,
    
    // Mines interactions
    MINES_REVEAL,
    MINES_CASHOUT,
    MINES_FLAG,
    
    // Poker interactions
    POKER_FOLD,
    POKER_CALL,
    POKER_RAISE,
    POKER_CHECK,
    POKER_ALL_IN,
    
    // Baccarat interactions
    BACCARAT_PLAYER_BET,
    BACCARAT_BANKER_BET,
    BACCARAT_TIE_BET,
    BACCARAT_DEAL,
    
    // Slots interactions
    SLOTS_SPIN,
    SLOTS_MAX_BET,
    
    // Keno interactions
    KENO_SELECT_NUMBER,
    KENO_CLEAR_SELECTION,
    KENO_PLAY,
    
    // Wheel of Fortune interactions
    WHEEL_SPIN,
    WHEEL_BET_SEGMENT,
    
    // General interactions
    INCREASE_BET,
    DECREASE_BET,
    CONFIRM_ACTION,
    CANCEL_ACTION,
    VIEW_STATS,
    HELP
}
