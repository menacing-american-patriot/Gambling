# 🎰 Gambling Plugin Configuration Guide

## Overview

The `config.yml` file allows you to customize every aspect of the casino building system and gambling plugin. This guide explains all configuration options and their effects.

## Configuration Sections

### 🏗️ **Casino Building System**

#### **Main Settings**
```yaml
casino-blocks:
  enabled: true  # Enable/disable the entire casino building system
```

#### **Visual Effects**
```yaml
visual-effects:
  holograms:
    enabled: true           # Show floating holograms above casino blocks
    height: 2.5            # Height above block (in blocks)
    floating-animation: true # Enable gentle floating animation
    update-interval: 2      # Ticks between hologram updates
  
  particles:
    enabled: true          # Show particle effects around casino blocks
    update-interval: 3     # Ticks between particle updates
    density: 1.0          # Particle density (0.1 = sparse, 2.0 = dense)
  
  player-effects:
    win-effects: true     # Show win animations for players
    loss-effects: true    # Show loss animations for players
    effect-duration: 60   # Duration in ticks (60 = 3 seconds)
```

#### **Physical Interactions**
```yaml
physical-interactions:
  enabled: true        # Enable buttons/levers around casino blocks
  auto-setup: true     # Automatically set up interactions when creating blocks
  range: 2.0          # Distance from casino block to place controls
  
  materials:           # Customize block types for different actions
    hit-button: "ACACIA_BUTTON"
    stand-button: "CRIMSON_BUTTON"
    double-button: "BIRCH_BUTTON"
    spin-lever: "LEVER"
    cashout-block: "EMERALD_BLOCK"
    bet-plate: "STONE_PRESSURE_PLATE"
```

#### **Default Settings**
```yaml
defaults:
  min-bet: 10.0       # Default minimum bet for new casino blocks
  max-bet: 1000.0     # Default maximum bet for new casino blocks
  enabled: true       # Default enabled state for new blocks
  
  block-types:        # Default block materials for each game
    blackjack: "ENCHANTING_TABLE"
    roulette: "CAULDRON"
    slots: "DISPENSER"
    # ... etc for all games
```

### 🎮 **Game-Specific Settings**

Each game can have its own configuration:

```yaml
games:
  blackjack:
    physical-controls: true    # Enable physical hit/stand buttons
    min-bet: 5.0              # Minimum bet for this game type
    max-bet: 10000.0          # Maximum bet for this game type
    allow-double-down: true   # Enable double down button
    allow-split: false        # Enable split button
  
  roulette:
    physical-controls: true
    min-bet: 1.0
    max-bet: 5000.0
    auto-spin-delay: 100      # Auto-spin after bet (ticks)
  
  crash:
    physical-controls: true
    min-bet: 10.0
    max-bet: 5000.0
    physical-cashout: true    # Enable physical cash-out button
```

### 🔐 **Permissions**

```yaml
permissions:
  per-game-permissions: true                    # Require specific permissions per game
  default-permission: "gambling.use"           # Base permission for casino blocks
  admin-permission: "gambling.admin.casinoblock" # Permission to manage blocks
```

**Permission Structure:**
- `gambling.use` - Basic casino block usage
- `gambling.use.blackjack` - Use blackjack casino blocks
- `gambling.use.roulette` - Use roulette casino blocks
- `gambling.admin.casinoblock` - Manage casino blocks

### 💰 **Economy Integration**

```yaml
economy:
  minimum-balance: 0.0      # Minimum balance required to use casino blocks
  usage-fee: 0.0           # Fee charged for using casino blocks (0.0 = disabled)
  fee-message: "&7A small usage fee of &a{fee} &7has been charged."
```

### 📊 **Performance Settings**

```yaml
performance:
  max-blocks-per-world: 100     # Maximum casino blocks per world
  max-active-sessions: 3        # Maximum active sessions per player
  session-timeout: 30           # Cleanup inactive sessions (minutes)
  particle-distance: 32         # Particle render distance (blocks)
  hologram-distance: 64         # Hologram render distance (blocks)
```

### 📝 **Logging and Debugging**

```yaml
logging:
  block-management: true        # Log casino block creation/removal
  player-interactions: false   # Log player interactions with blocks
  physical-interactions: false # Log button/lever interactions
  debug: false                 # Enable verbose debug logging
```

### 💬 **Messages and Localization**

```yaml
messages:
  block-created: "&a✓ Casino block created successfully!"
  block-removed: "&a✓ Casino block removed successfully!"
  insufficient-funds: "&cYou need at least &a{min_bet} &cto play this game."
  no-permission: "&cYou don't have permission to use this casino game."
  
  hologram-format:
    - "{game_name}"
    - "&f{block_name}"
    - "&aMin: {min_bet} | Max: {max_bet}"
    - "&eRight-click to play!"
```

**Available Placeholders:**
- `{game_name}` - Formatted game name with icon
- `{block_name}` - Custom name of the casino block
- `{min_bet}` - Minimum bet amount (formatted)
- `{max_bet}` - Maximum bet amount (formatted)
- `{fee}` - Usage fee amount (formatted)

### 🔧 **Advanced Settings**

#### **Database Integration**
```yaml
advanced:
  use-database: false    # Use database instead of YAML files
  database:
    host: "localhost"
    port: 3306
    database: "gambling"
    username: "gambling_user"
    password: "password"
    table-prefix: "gambling_"
```

#### **Plugin Integrations**
```yaml
integrations:
  worldguard: true        # Respect WorldGuard region flags
  griefprevention: true   # Respect GriefPrevention claims
  plotsquared: true       # Respect PlotSquared plots
```

#### **Backup Settings**
```yaml
backup:
  enabled: true          # Auto-backup casino blocks
  interval: 24           # Backup interval (hours)
  keep-backups: 7        # Number of backup files to keep
```

## Configuration Tips

### **Performance Optimization**

1. **Reduce Particle Density**: Set `density: 0.5` for better performance
2. **Increase Update Intervals**: Higher values = less frequent updates = better performance
3. **Limit Render Distances**: Reduce `particle-distance` and `hologram-distance`
4. **Disable Unused Features**: Turn off holograms or particles if not needed

### **Customization Ideas**

1. **Themed Casinos**: Use different block types and materials for different themes
2. **VIP Areas**: Set higher min/max bets for exclusive casino blocks
3. **Beginner Areas**: Lower bet limits for new players
4. **Custom Messages**: Translate messages to your server's language

### **Security Settings**

1. **Permission Control**: Enable `per-game-permissions` for fine-grained access
2. **Usage Fees**: Set small fees to prevent spam usage
3. **Session Limits**: Limit active sessions to prevent resource abuse

## Reloading Configuration

Use the command `/casinoblock reload` to reload the configuration without restarting the server.

**Note**: Some changes (like database settings) may require a server restart to take effect.

## Example Configurations

### **High-Performance Server**
```yaml
visual-effects:
  particles:
    density: 0.3
    update-interval: 5
  holograms:
    update-interval: 5

performance:
  particle-distance: 16
  hologram-distance: 32
```

### **Immersive Experience**
```yaml
visual-effects:
  particles:
    density: 2.0
    update-interval: 1
  player-effects:
    effect-duration: 120

physical-interactions:
  auto-setup: true
  range: 3.0
```

### **Economy-Focused**
```yaml
economy:
  minimum-balance: 100.0
  usage-fee: 5.0

games:
  blackjack:
    min-bet: 50.0
    max-bet: 50000.0
  roulette:
    min-bet: 25.0
    max-bet: 25000.0
```

## Troubleshooting

### **Common Issues**

1. **Holograms Not Showing**: Check `holograms.enabled` and render distance
2. **Particles Lagging**: Reduce `density` and increase `update-interval`
3. **Permissions Not Working**: Verify permission plugin compatibility
4. **Physical Interactions Broken**: Check `physical-interactions.enabled`

### **Debug Mode**

Enable debug logging to troubleshoot issues:
```yaml
logging:
  debug: true
  player-interactions: true
  physical-interactions: true
```

This will provide detailed logs about what the plugin is doing.

## Support

If you need help with configuration, check the plugin documentation or contact support with your specific `config.yml` settings and any error messages.
