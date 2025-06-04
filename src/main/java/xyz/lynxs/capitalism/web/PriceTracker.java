package xyz.lynxs.capitalism.web;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;

import xyz.lynxs.capitalism.Capitalism;
import xyz.lynxs.capitalism.Config;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PriceTracker {
    private  final Map<Item, Double> itemPrices = new ConcurrentHashMap<>();
    private final List<PriceHistory> priceHistory = new CopyOnWriteArrayList<>();
    private  final Map<Item, Double> priceFluctuations = new ConcurrentHashMap<>();
    private  Instant lastUpdate = Instant.now();

    // Initialize with config prices
    public  void initializeFromConfig(MinecraftServer server) {
        Config.getItemPrices().forEach(this::updatePrice);
        Capitalism.LOGGER.info("Initialized price tracker with {} items", itemPrices.size());
        Runnable UpdatePrices = this::updatePrice;
        server.addTickable(UpdatePrices);
    }

    private  void updatePrice(Item item, Double aDouble) {
        updatePrice(item, aDouble, true);
    }

    // Update price with optional fluctuation
    public void updatePrice() {
        itemPrices.forEach((key, value) -> updatePrice(key, value, true));
    }

    public  void updatePrice(Item item, double price, boolean applyFluctuation) {
        double newPrice = price;

        if (applyFluctuation && priceFluctuations.containsKey(item)) {
            double fluctuation = priceFluctuations.get(item);
            double fluctuationAmount = price * (fluctuation / 100);
            newPrice = price + (Math.random() * fluctuationAmount * 2 - fluctuationAmount);
            newPrice = Math.max(newPrice, 0.01); // Ensure price doesn't go below 0.01
        }

        itemPrices.put(item, newPrice);
        priceHistory.add(new PriceHistory(item, newPrice, System.currentTimeMillis()));
        lastUpdate = Instant.now();

        // Keep history size manageable
        if (priceHistory.size() > 32) {
            priceHistory.removeFirst();
        }
    }

    // Batch update prices
    public void updatePrices(Map<Item, Double> newPrices) {
        newPrices.forEach((item, price) -> updatePrice(item, price, true));
    }

    // Get current price with optional formatted string
    public double getCurrentPrice(Item item) {
        return itemPrices.getOrDefault(item, -1.0);
    }

    public String getFormattedPrice(Item item) {
        return String.format("%.2f", getCurrentPrice(item));
    }

    // Price history methods
    public List<PriceHistory> getPriceHistory(Item item) {
        return priceHistory.stream()
                .filter(ph -> ph.item().equals(item))
                .collect(Collectors.toList());
    }

    public List<PriceHistory> getRecentPriceHistory(Item item, int maxEntries) {
        return getPriceHistory(item).stream()
                .sorted(Comparator.comparingLong(PriceHistory::timestamp).reversed())
                .limit(maxEntries)
                .collect(Collectors.toList());
    }

    // Market data methods
    public Map<Item, Double> getAllPrices() {
        return Collections.unmodifiableMap(itemPrices);
    }

    public Map<Item, Double> getPriceChanges(Instant since) {
        Map<Item, Double> changes = new HashMap<>();
        itemPrices.keySet().forEach(item -> {
            Optional<PriceHistory> oldPrice = priceHistory.stream()
                    .filter(ph -> ph.item().equals(item))
                    .filter(ph -> Instant.ofEpochMilli(ph.timestamp()).isBefore(since))
                    .max(Comparator.comparingLong(PriceHistory::timestamp));

            oldPrice.ifPresent(ph ->
                    changes.put(item, itemPrices.get(item) - ph.price()));
        });
        return changes;
    }

    // Price fluctuation configuration
    public void setPriceFluctuation(Item item, double percentage) {
        priceFluctuations.put(item, percentage);
    }

    public void setDefaultFluctuation(double percentage) {
        itemPrices.keySet().forEach(item ->
                priceFluctuations.put(item, percentage));
    }

    // Market operations
    public void applyMarketFluctuation() {
        itemPrices.forEach((item, price) ->
                updatePrice(item, price, true));
    }

    // Getters
    public Instant getLastUpdateTime() {
        return lastUpdate;
    }

    public boolean hasPrice(Item item) {
        return itemPrices.containsKey(item);
    }

    // Record for price history
    public record PriceHistory(Item item, double price, long timestamp) {
        public String getFormattedTimestamp() {
            return Instant.ofEpochMilli(timestamp).toString();
        }
    }
}