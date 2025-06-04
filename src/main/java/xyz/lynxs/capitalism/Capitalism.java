package xyz.lynxs.capitalism;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import xyz.lynxs.capitalism.web.PriceTracker;

import java.net.URI;

@Mod(Capitalism.MODID)
public class Capitalism {

    public static final URI WEB_FILES = URI.create("https://raw.githubusercontent.com/ly-nxs/web/refs/heads/main/");

    public static final String MODID = "capitalism";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static PriceTracker priceTracker = null;
    public static Holder<Attribute> BALANCE = null;

    public Capitalism(IEventBus modEventBus, ModContainer modContainer) {
        priceTracker = new PriceTracker();

        // Register config first
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        // Register attributes
        DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(
                BuiltInRegistries.ATTRIBUTE, Capitalism.MODID);
        BALANCE = ATTRIBUTES.register("balance", () -> new RangedAttribute(
                "attribute.capitalism.balance",
                0.0,
                0.0,
                Double.MAX_VALUE
        ).setSyncable(true));

        // Register the deferred register
        ATTRIBUTES.register(modEventBus);

        // Register event listeners

        modEventBus.addListener(this::modifyDefaultAttributes); // Moved here

        NeoForge.EVENT_BUS.register(Listeners.class);
    }

    @SubscribeEvent
    public void modifyDefaultAttributes(EntityAttributeModificationEvent event) {
        // Add your balance attribute to players
        if (!event.has(EntityType.PLAYER, BALANCE)) {
            event.add(EntityType.PLAYER, BALANCE);
        }
    }



}


