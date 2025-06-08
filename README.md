# Capitalism Mod - Minecraft Economy System

A server-side economy mod for Minecraft that adds player balances, item trading, and web integration.

## Features
- 💰 Player balance tracking with persistent storage

- 🛒 Buy/sell system with dynamic pricing

- 🌐 Optional web interface for economic tracking

- 📊 Server-side only - no client mod required

- ⚙️ Fully configurable economy settings

## Installation
1. Download the latest .jar file from Releases
2. Place the file in your server's mods folder
3. Restart the server

## Configuration
Edit config/capitalism-server.toml:

```toml
#List of items to track with their prices in format 'modid:item=price'
#Example: minecraft:diamond=100.0, minecraft:gold_ingot=50.0
#This is just the initial price
itemPrices = ["minecraft:diamond=100.0", "minecraft:gold_ingot=50.0", "minecraft:iron_ingot=25.0"]
#Port for the web interface
#Default: 8080
#Range: 1024 ~ 49151
# Default: 8080
# Range: 1024 ~ 49151
webPort = 8080
#Whether to enable the web interface
enableWeb = true
#How often to update prices (in seconds)
#Default: 300 (5 minutes)
#Range: 10 ~ 86400 (1 day)
# Default: 300
# Range: 10 ~ 86400
updateInterval = 300
#Maximum percentage price can fluctuate each update
#Default: 5.0
#Range: 0.0 ~ 100.0
# Default: 5.0
# Range: 0.0 ~ 100.0
priceFluctuation = 5.0
#Maximum number of history entries to keep per item
#Default: 1000
# Default: 1000
# Range: 10 ~ 10000
maxHistoryEntries = 1000
#Number of threads to use
#Default: 8
# Default: 1
# Range: 1 ~ 1024
threads = 1
#Path for the web resources
webPath = "./config/capitalism"
#Where should players see their balance?
#'tablist': display on tablist footer, 'chat': only through /balance command
balanceDisplay = "tablist"
```

### Commands
Player Commands
- `/balance` - Check your current balance
- `/sell <amount>` - Sell items in your main hand
- `/pay <player> <amount>` - Transfer money to another player

### Admin Commands
- `/capitalism reload-web` - Reload web interface
- `/capitalism setbalance <player> <amount>` - Set a player's balance

## Web Interface
Access the web dashboard at http://yourserver:8080 when enabled.



### API for Developers (TODO)
Access player balances from other mods:

```java
double balance = Capitalism.getPlayerBalance(player);
Capitalism.addToBalance(player, 100);
```

### FAQ
Q: Does this require client mods?
A: *No, this works 100% server-side.*

Q: How are balances stored?
A: *Balances persist through player attributes and save with player data.*

Q: Can I customize prices?
A: *Yes, edit config/capitalism/prices.json.*

### Support
- Report issues on GitHub Issues.
- [Join our Discord for help](https://inv.lynxs.xyz).

### License
*This mod is available under the MIT License.*

- Version: 1.0.0
- Minecraft Versions: 1.21.1
- Dependencies: NeoForge
