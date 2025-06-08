package xyz.lynxs.capitalism;


import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Commands {
    private final Map<String, Pair<@Nullable String,@Nullable ArgumentType<?>>> cmds;
    private final Map<String, Integer> permLevels;

    private final Map<String, Consumer<CommandContext<CommandSourceStack>>> functions;
    private final String base;



    public Commands(String base, int permLevel, Consumer<CommandContext<CommandSourceStack>> baseFunction, @Nullable String baseArg, @Nullable ArgumentType<?> argType){
        cmds = new HashMap<>();
        functions = new HashMap<>();
        permLevels = new HashMap<>();
        this.base = base;

        cmds.put(base, new Pair<>(baseArg, argType));
        functions.put(base, baseFunction);
        permLevels.put(base, permLevel);

    }

    public void addSubCommand(String name, int permLevel, Consumer<CommandContext<CommandSourceStack>> function, @Nullable String arg, @Nullable ArgumentType<?> argType){
        cmds.put(base + name, new Pair<>(arg, argType));
        functions.put(base + name, function);
        permLevels.put(base + name, permLevel);
    }

    public LiteralArgumentBuilder<CommandSourceStack> getCommand(String name){
        LiteralArgumentBuilder<CommandSourceStack> base = net.minecraft.commands.Commands.literal(this.base);

        if(name.equals(this.base)){
            Pair<@Nullable String, @Nullable ArgumentType<?>> pair = cmds.get(name);
            if(!(pair.getA() == null) && !(pair.getB() == null) ) {
                RequiredArgumentBuilder<CommandSourceStack, ?> requiredArgumentBuilder = net.minecraft.commands.Commands.argument(pair.getA(), pair.getB());
                return base.then(requiredArgumentBuilder
                        .requires(source -> source.hasPermission(permLevels.get(name)))
                        .executes(context -> {functions.get(name).accept(context); return 1;}));
            }
            return base
                    .requires(source -> source.hasPermission(permLevels.get(name)))
                    .executes(context -> {functions.get(name).accept(context); return 1;});
        }
        LiteralArgumentBuilder<CommandSourceStack> sub = net.minecraft.commands.Commands.literal(name);
        Pair<@Nullable String, @Nullable ArgumentType<?>> pair = cmds.get(this.base + name);
        if(!(pair.getA() == null) && !(pair.getB() == null) ) {
            RequiredArgumentBuilder<CommandSourceStack, ?> requiredArgumentBuilder = net.minecraft.commands.Commands.argument(pair.getA(), pair.getB());
            return base.then(sub.then(requiredArgumentBuilder
                    .requires(source -> source.hasPermission(permLevels.get(this.base + name)))
                    .executes(context ->{ functions.get(this.base + name).accept(context); return  1;})));
        }
        return base.then(sub
                .requires(source -> source.hasPermission(permLevels.get(this.base + name)))
                .executes(context -> {functions.get(this.base + name).accept(context); return 1;}));

    }
    public LiteralArgumentBuilder<CommandSourceStack> getCommandWithoutArgs(String name, Consumer<CommandContext<CommandSourceStack>> consumer,int perm){
        LiteralArgumentBuilder<CommandSourceStack> base = net.minecraft.commands.Commands.literal(this.base);

        if(name.equals(this.base)){
            return base
                    .requires(source -> source.hasPermission(perm))
                    .executes(context -> {consumer.accept(context); return 1;});
        }
        LiteralArgumentBuilder<CommandSourceStack> sub = net.minecraft.commands.Commands.literal(name);
        return base.then(sub
                .requires(source -> source.hasPermission(perm))
                .executes(context -> {consumer.accept(context); return 1;}));

    }







}
