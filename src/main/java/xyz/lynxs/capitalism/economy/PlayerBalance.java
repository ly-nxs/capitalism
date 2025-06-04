package xyz.lynxs.capitalism.economy;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.lynxs.capitalism.Capitalism;

// PlayerBalance.java
public class PlayerBalance {


    private long balance; // Using long to avoid floating-point issues (store cents/pennies)

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public void addBalance(long amount) {
        this.balance += amount;
    }

    public boolean subtractBalance(long amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

}