package xyz.lynxs.capitalism;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Capitalism.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Config Values
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_PRICES;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_BALANCE_DISPLAY;
    public static final ModConfigSpec.IntValue WEB_PORT;
    public static final ModConfigSpec.BooleanValue ENABLE_WEB;
    public static final ModConfigSpec.ConfigValue<String> WEB_PATH;
    public static final ModConfigSpec.IntValue UPDATE_INTERVAL;
    public static final ModConfigSpec.DoubleValue PRICE_FLUCTUATION;
    public static final ModConfigSpec.IntValue MAX_HISTORY_ENTRIES;
    public static final ModConfigSpec.IntValue THREADS;

    // The SPEC instance
    public static final ModConfigSpec SPEC;

    // Runtime storage
    private static Map<Item, Double> loadedPrices = new ConcurrentHashMap<>();

    static {
        // Define all config entries
        ITEM_PRICES = BUILDER.comment(
                        "List of items to track with their prices in format 'modid:item=price'",
                        "Example: minecraft:diamond=100.0, minecraft:gold_ingot=50.0",
                        "This is just the initial price")
                .defineList("itemPrices", Arrays.asList(
                        "minecraft:diamond=100.0",
                        "minecraft:gold_ingot=50.0",
                        "minecraft:iron_ingot=25.0"
                ), Config::validateItemEntry);
        PLAYER_BALANCE_DISPLAY = BUILDER.comment(
                "Where should players see their balance?",
                "'tablist': display on tablist footer, 'chat': only through /balance command"
        ).defineInList("balanceDisplay","tablist", Arrays.asList("tablist","chat"));
        WEB_PORT = BUILDER.comment(
                        "Port for the web interface",
                        "Default: 8080",
                        "Range: 1024 ~ 49151")
                .defineInRange("webPort", 8080, 1024, 49151);

        ENABLE_WEB = BUILDER.comment(
                        "Whether to enable the web interface")
                .define("enableWeb", true);
        WEB_PATH = BUILDER.comment(
                "Path for the web resources")
                .define("webPath", "./config/capitalism/");
        UPDATE_INTERVAL = BUILDER.comment(
                        "How often to update prices (in seconds)",
                        "Default: 300 (5 minutes)",
                        "Range: 10 ~ 86400 (1 day)")
                .defineInRange("updateInterval", 300, 10, 86400);

        PRICE_FLUCTUATION = BUILDER.comment(
                        "Maximum percentage price can fluctuate each update",
                        "Default: 5.0",
                        "Range: 0.0 ~ 100.0")
                .defineInRange("priceFluctuation", 5.0, 0.0, 100.0);

        MAX_HISTORY_ENTRIES = BUILDER.comment(
                        "Maximum number of history entries to keep per item",
                        "Default: 1000")
                .defineInRange("maxHistoryEntries", 1000, 10, 10000);

        THREADS = BUILDER.comment(
                "Number of threads to use",
                "Default: 8"
        ).defineInRange("threads",1, 1, 1024);

        // Build the spec
        SPEC = BUILDER.build();
    }

    private static boolean validateItemEntry(final Object obj) {
        if (!(obj instanceof String entry)) {
            Capitalism.LOGGER.error("Config entry is not a string: {}", obj);
            return false;
        }

        String[] parts = entry.split("=");
        if (parts.length != 2) {
            Capitalism.LOGGER.error("Invalid price entry format (should be 'modid:item=price'): {}", entry);
            return false;
        }

        try {
            ResourceLocation itemId = ResourceLocation.parse(parts[0]);
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                Capitalism.LOGGER.error("Item not found in registry: {}", parts[0]);
                return false;
            }
        } catch (Exception e) {
            Capitalism.LOGGER.error("Invalid item ID format: {}", parts[0]);
            return false;
        }

        try {
            double price = Double.parseDouble(parts[1]);
            if (price <= 0) {
                Capitalism.LOGGER.error("Price must be positive: {}", parts[1]);
                return false;
            }
        } catch (NumberFormatException e) {
            Capitalism.LOGGER.error("Invalid price format (must be a number): {}", parts[1]);
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(Capitalism.MODID)) {
            loadConfigValues();
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(Capitalism.MODID)) {
            loadConfigValues();
        }
    }

    private static void loadConfigValues() {
        Map<Item, Double> newPrices = new ConcurrentHashMap<>();

        for (String entry : ITEM_PRICES.get()) {
            try {
                String[] parts = entry.split("=");
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(parts[0]));
                if (item != null) {
                    newPrices.put(item, Double.parseDouble(parts[1]));
                }
            } catch (Exception e) {
                Capitalism.LOGGER.error("Invalid config entry: {}", entry, e);
            }
        }

        loadedPrices = newPrices;
        Capitalism.LOGGER.info("Loaded config with {} items", loadedPrices.size());
    }

    // Public access methods
    public static Map<Item, Double> getItemPrices() {
        return Collections.unmodifiableMap(loadedPrices);
    }

    public static int getWebPort() {
        return WEB_PORT.get();
    }

    public static boolean isWebEnabled() {
        return ENABLE_WEB.get();
    }

    public static int getUpdateInterval() {
        return UPDATE_INTERVAL.get();
    }

    public static double getPriceFluctuation() {
        return PRICE_FLUCTUATION.get();
    }

    public static int getMaxHistoryEntries() {
        return MAX_HISTORY_ENTRIES.get();
    }

    public static Path getWebPath(){
        return Path.of(WEB_PATH.get());
    }

    public static int getThreads(){
        return THREADS.get();
    }
    public static String getPlayerDisplay(){
        return PLAYER_BALANCE_DISPLAY.get();
    }
}