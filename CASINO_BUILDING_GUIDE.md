# 🎰 Casino Building System Guide

## Overview

The Casino Building System allows you to create physical casino blocks in your Minecraft world that players can right-click to play games. This provides a more immersive and integrated experience compared to just using commands and GUIs.

## Features

### ✨ **Physical Casino Blocks**
- Right-click blocks to start games
- Visual indicators with holograms and particles
- Configurable bet limits per block
- Support for all gambling games

### 🎮 **Physical Game Interactions**
For supported games, players can use physical buttons, levers, and pressure plates around casino blocks for game actions:

- **Blackjack**: Hit/Stand/Double Down buttons around the table
- **Roulette**: Spin lever and betting pressure plates
- **Craps**: Roll dice button and betting areas
- **Crash**: Cash out button and bet adjustment controls
- **Plinko**: Drop zones above the board
- **Mines**: Grid of pressure plates for revealing squares

### 🎨 **Visual Effects**
- Floating holograms with game info and bet limits
- Game-specific particle effects
- Win/loss animations for players
- Smooth floating animations

## Setup Commands

### **Permission Required**: `gambling.admin.casinoblock`

### **Create a Casino Block**
```
/casinoblock create <gameType> <name> <minBet> <maxBet> <blockType>
```

**Example:**
```
/casinoblock create blackjack "VIP Blackjack Table" 100 5000 enchanting_table
```

**Available Game Types:**
- `blackjack` - Classic 21 card game
- `roulette` - Spin the wheel
- `slots` - Slot machine
- `poker` - Texas Hold'em multiplayer
- `baccarat` - Player vs Banker
- `craps` - Dice rolling game
- `keno` - Number selection lottery
- `crash` - Multiplier crash game
- `plinko` - Ball drop game
- `wheel_of_fortune` - Spin for prizes
- `mines` - Find gems, avoid mines
- `coinflip` - Simple heads/tails

**Recommended Block Types:**
- **Blackjack**: `enchanting_table` or `cartography_table`
- **Roulette**: `cauldron`
- **Slots**: `dispenser`
- **Poker**: `crafting_table`
- **Craps**: `smooth_stone_slab`
- **Others**: See the game type defaults

### **Remove a Casino Block**
```
/casinoblock remove
```
*Look at the casino block you want to remove*

### **List All Casino Blocks**
```
/casinoblock list
```

### **Get Block Information**
```
/casinoblock info
```
*Look at a casino block to see its details*

### **Set Up Physical Interactions**
```
/casinoblock setup
```
*Look at a casino block to set up buttons/levers around it*

### **Reload Configuration**
```
/casinoblock reload
```

## How It Works

### **For Players**

1. **Find a Casino Block**: Look for blocks with floating holograms and particle effects
2. **Right-Click to Play**: Right-click the casino block to start the game
3. **Use Physical Controls**: For supported games, use buttons and levers around the block for game actions
4. **Enjoy the Experience**: Visual effects and animations enhance the gameplay

### **For Admins**

1. **Choose Location**: Find a good spot for your casino
2. **Place Block**: Look at where you want the casino block
3. **Create Casino Block**: Use the create command with your desired settings
4. **Customize**: Adjust bet limits and block types as needed
5. **Build Around It**: Create a casino building around your blocks

## Game-Specific Physical Interactions

### **Blackjack** ♠️
- **Green Button (Right)**: Hit - Draw another card
- **Red Button (Left)**: Stand - End your turn
- **Yellow Button (Front)**: Double Down - Double bet and draw one card

### **Roulette** 🎯
- **Lever (Above)**: Spin the wheel
- **Pressure Plates (Around)**: Place bets on different areas

### **Craps** 🎲
- **Stone Button (Front)**: Roll the dice
- **Green Button (Left)**: Pass Line bet
- **Red Button (Right)**: Don't Pass bet

### **Crash** 🚀
- **Emerald Block (Above)**: Cash out before crash
- **Green Button (Right)**: Increase bet amount
- **Red Button (Left)**: Decrease bet amount

### **Plinko** ⚪
- **Pressure Plates (Above)**: Drop ball at different positions (Slots 0-8)

### **Mines** 💣
- **Emerald Block (Above)**: Cash out current winnings
- **Heavy Pressure Plates (Grid)**: Reveal squares in 3x3 grid

## Building Tips

### **Casino Layout Ideas**

1. **Blackjack Area**
   - Use enchanting tables as the main blocks
   - Surround with buttons for hit/stand
   - Add chairs (stairs) around the table
   - Use carpet for betting areas

2. **Roulette Section**
   - Central cauldron as the wheel
   - Pressure plates in a circle for betting
   - Lever above for spinning
   - Red and black carpet patterns

3. **Slot Machine Row**
   - Line up dispensers
   - Add levers on the sides
   - Use gold blocks for decoration
   - Create coin slot effects with hoppers

4. **Poker Room**
   - Crafting tables as poker tables
   - Chairs around each table
   - Private rooms for high-stakes games

### **Decoration Ideas**

- **Lighting**: Use glowstone, sea lanterns, or redstone lamps
- **Flooring**: Quartz, polished stone, or colored concrete
- **Walls**: Use banners with casino-themed patterns
- **Atmosphere**: Add music with note blocks
- **Security**: Iron golems as casino security

## Permissions

- `gambling.use` - Basic permission to use casino blocks
- `gambling.use.<gametype>` - Permission for specific games (e.g., `gambling.use.blackjack`)
- `gambling.admin.casinoblock` - Permission to manage casino blocks

## Configuration

Casino blocks are automatically saved to `plugins/Gambling/casino-blocks.yml` and persist across server restarts.

## Troubleshooting

### **Block Not Working**
- Check if the block is registered: `/casinoblock info`
- Verify player has permission: `gambling.use`
- Ensure player has enough money for minimum bet

### **Physical Interactions Not Working**
- Run `/casinoblock setup` while looking at the block
- Check if the game type supports physical interactions
- Verify the interaction blocks are properly placed

### **Visual Effects Missing**
- Restart the server to reload visual effects
- Check if the block location is properly loaded
- Verify no other plugins are interfering with particles

## Examples

### **Create a High-Roller Blackjack Table**
```
/casinoblock create blackjack "High Roller Blackjack" 1000 50000 enchanting_table
```

### **Set Up a Beginner Roulette Wheel**
```
/casinoblock create roulette "Beginner Roulette" 10 1000 cauldron
```

### **Create a Slot Machine**
```
/casinoblock create slots "Lucky Slots" 50 2500 dispenser
```

## Integration with Existing Systems

The casino building system seamlessly integrates with your existing gambling plugin:
- All game logic remains the same
- Economy integration works as before
- Leaderboards and statistics are maintained
- GUI systems are still available as fallbacks

This system enhances the player experience while maintaining all the functionality you already have!
