package xyz.lynxs.capitalism.web;

import net.minecraft.world.item.Item;

public record PriceHistory(Item item, double price, long timestamp) {}
