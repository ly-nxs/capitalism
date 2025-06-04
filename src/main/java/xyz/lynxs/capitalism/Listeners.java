package xyz.lynxs.capitalism;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import xyz.lynxs.capitalism.web.WebServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("capitalism")
                        .then(net.minecraft.commands.Commands.literal("reload-web")
                                .requires(source -> source.hasPermission(2)) // Requires OP permission
                                .executes(context -> {
                                    // Reload web components
                                    WebServer.reload(priceTracker);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Reloaded web interface components"),
                                            false
                                    );
                                    return 1;
                                })
                        ));
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("sell")
                        .then(net.minecraft.commands.Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        context.getSource().sendFailure(Component.literal("Command must be run by a player"));
                                        return 0;
                                    }

                                    // Get the attribute instance safely
                                    AttributeInstance balanceAttribute = player.getAttribute(BALANCE);
                                    if (balanceAttribute == null) {
                                        context.getSource().sendFailure(Component.literal("Balance system not initialized!"));
                                        return 0;
                                    }

                                    ItemStack itemStack = player.getMainHandItem();
                                    double pricePer = priceTracker.getCurrentPrice(itemStack.getItem());

                                    if (pricePer == -1.0) {
                                        context.getSource().sendFailure(Component.literal("Item currently not traded!"));
                                        return 0;
                                    }

                                    int numItems = itemStack.getCount();
                                    int amount = context.getArgument("amount", Integer.class);
                                    amount = Math.min(amount, numItems);
                                    double total = pricePer * amount;

                                    // Update balance
                                    double currentBalance = balanceAttribute.getValue();
                                    balanceAttribute.setBaseValue(currentBalance + total);
                                    sendTopRightBalance(player, balanceAttribute.getValue());
                                    // Update item stack
                                    itemStack.setCount(numItems - amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(String.format("$%.2f was deposited! New balance: $%.2f",
                                                    total, balanceAttribute.getValue())),
                                            false
                                    );
                                    return 1;
                                })
                        ));
    }
    //TODO: make modes
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeInstance balanceAttr = player.getAttribute(Capitalism.BALANCE);
            if (balanceAttr != null) {
                sendTopRightBalance(player, balanceAttr.getValue());
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

    private static void sendTopRightBalance(ServerPlayer player, double balance) {
        // Create the balance text component
        Component balanceText = Component.literal(String.format("$%,.2f", balance))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

       player.setTabListFooter(balanceText);
    }
}
