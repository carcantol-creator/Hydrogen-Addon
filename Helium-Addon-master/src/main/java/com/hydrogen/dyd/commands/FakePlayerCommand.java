package com.hydrogen.dyd.commands;

import com.hydrogen.dyd.modules.FakePlayer;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand() {
        super("fakeplayer", "Controla el fake player avanzado", "fp");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("spawn").executes(context -> {
            Modules.get().get(FakePlayer.class).spawn();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("despawn").executes(context -> {
            Modules.get().get(FakePlayer.class).despawn();
            return SINGLE_SUCCESS;
        }));
    }
}
