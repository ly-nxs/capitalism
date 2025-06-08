package xyz.lynxs.capitalism;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import xyz.lynxs.capitalism.web.WebServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static xyz.lynxs.capitalism.Capitalism.*;

public class Listeners {


    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        priceTracker.initializeFromConfig(event.getServer());
        priceTracker.setDefaultFluctuation(10.0);
        if (Config.isWebEnabled()) {
            startWebServer(Config.getWebPath());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        Commands capitalism = new Commands("capitalism", 0, (context) -> {
            Component about = Component.empty()
                    .append(Component.literal("<--- Capitalism --->")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                    .append("\n")
                                    .append(Component.literal("Minecraft Economy for ")
                                    .withStyle(ChatFormatting.RESET, ChatFormatting.GRAY))
                                    .append(Component.literal("NeoForge")
                                    .withStyle(ChatFormatting.RED))
                                    .append("\n")
                                    .append(Component.literal("- By: lynx -")
                                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
            context.getSource().sendSystemMessage(about);
        }, null, null);

        event.getDispatcher().register(capitalism.getCommand("capitalism"));

        capitalism.addSubCommand("reload", 2, (context -> {
            WebServer.reload(priceTracker);
            context.getSource().sendSuccess(() -> Component.literal("Reloaded Web"), true);
        }), null, null);

        event.getDispatcher().register(capitalism.getCommand("reload"));

        Commands sell = new Commands("sell", 0, (context) -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("Command must be run by a player"));
            }

            // Get the attribute instance safely
           else {
                AttributeInstance balanceAttribute = player.getAttribute(BALANCE);
                if (balanceAttribute == null) {
                    context.getSource().sendFailure(Component.literal("Balance system not initialized!"));
                }

                ItemStack itemStack = player.getMainHandItem();

                double pricePer = priceTracker.getCurrentPrice(itemStack.getItem());

                if (itemStack.getItem() == Items.AIR) {
                    context.getSource().sendFailure(Component.literal("You aren't holding anything!").withStyle(ChatFormatting.RED));
                } else if (pricePer == -1.0) {
                    context.getSource().sendFailure(Component.literal("Item currently not traded!").withStyle(ChatFormatting.RED));
                } else {

                    int numItems = itemStack.getCount();
                    int amount = context.getArgument("amount", Integer.class);
                    amount = Math.min(amount, numItems);
                    double total = pricePer * amount;

                    // Update balance
                    assert balanceAttribute != null;
                    double currentBalance = balanceAttribute.getValue();
                    balanceAttribute.setBaseValue(currentBalance + total);
                    sendBalance(player, balanceAttribute.getValue());
                    // Update item stack
                    itemStack.setCount(numItems - amount);

                    context.getSource().sendSuccess(
                            () -> Component.literal(String.format("$%.2f was deposited! New balance: $%.2f",
                                    total, balanceAttribute.getValue())),
                            false
                    );
                }
            }
        }, "amount", IntegerArgumentType.integer());

        event.getDispatcher().register(sell.getCommand("sell"));

        event.getDispatcher().register(sell.getCommandWithoutArgs("sell",(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("Command must be run by a player"));
            }

            // Get the attribute instance safely
            else {
                AttributeInstance balanceAttribute = player.getAttribute(BALANCE);
                if (balanceAttribute == null) {
                    context.getSource().sendFailure(Component.literal("Balance system not initialized!"));
                }

                ItemStack itemStack = player.getMainHandItem();

                double pricePer = priceTracker.getCurrentPrice(itemStack.getItem());

                if (itemStack.getItem() == Items.AIR)
                    context.getSource().sendFailure(Component.literal("You aren't holding anything!").withStyle(ChatFormatting.RED));

                else if (pricePer == -1.0)
                    context.getSource().sendFailure(Component.literal("Item currently not traded!").withStyle(ChatFormatting.RED));
                else
                    context.getSource().sendSuccess(() -> {
                                assert balanceAttribute != null;
                                return Component.literal(String.format("Price is currently $%.2f per Item",
                                        balanceAttribute.getValue()));
                            },
                            false);
            }

        }), 0));

        Commands balance = new Commands("balance", 0, (context) -> {
            ServerPlayer player = null;
            try {
                player = EntityArgument.getPlayer(context, "player");
            } catch (CommandSyntaxException e) {
                context.getSource().sendFailure(Component.literal("Please enter a valid player"));
            }
            if (player == null) {
                context.getSource().sendFailure(Component.literal("Command must be run by a player"));
            }

            // Get the attribute instance safely
            else {
                AttributeInstance balanceAttribute = player.getAttribute(BALANCE);
                if (balanceAttribute == null) {
                    context.getSource().sendFailure(Component.literal("Balance system not initialized!"));
                }
            }
        }, "player", EntityArgument.player());

        event.getDispatcher().register(balance.getCommand("balance"));
        event.getDispatcher().register(balance.getCommandWithoutArgs("balance",
                (context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("Command must be run by a player"));
                    }
                    else {
                        double bal = Objects.requireNonNull(player.getAttribute(BALANCE)).getValue();
                        player.sendSystemMessage(Component.literal(String.format("You have $%.2f", bal)));
                    }
        }),0));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeInstance balanceAttr = player.getAttribute(Capitalism.BALANCE);
            if (balanceAttr != null) {
                sendBalance(player, balanceAttr.getValue());
            }
        }
    }
    private static synchronized void startWebServer(Path path) {
        writeWebFiles(path);

        try {
            WebServer.start(priceTracker);
            Capitalism.LOGGER.info("Web server started on port {}", Config.getWebPort());
        } catch (Exception e) {
            Capitalism.LOGGER.error("Failed to start web server", e);
        }
    }
    public static void writeWebFiles(Path destinationPath) {
        if (Files.notExists(destinationPath)) {
            String[] files = {"index.html", "styles.css", "app.js"};
            try{
                Files.createDirectory(destinationPath);
                for(String file : files){
                    try(InputStream inputStream = WEB_FILES.resolve(file).toURL().openStream()){
                        Files.copy(inputStream, destinationPath.resolve(file));
                    }
                }

            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private static void sendBalance(ServerPlayer player, double balance) {
        // Create the balance text component
        Component balanceText = Component.literal(String.format("$%,.2f", balance))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if(Config.getPlayerDisplay().equalsIgnoreCase("tablist")){
            player.setTabListFooter(balanceText);
        }
    }
}
