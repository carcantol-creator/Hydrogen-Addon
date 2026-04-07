package com.helium.commands;

import com.helium.modules.FakePlayer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand() {
        super("fakeplayer", "Controla el bot de Helium", "fp");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Comando .fp spawn
        builder.then(literal("spawn").executes(context -> {
            FakePlayer fp = Modules.get().get(FakePlayer.class);
            if (fp.isActive()) {
                fp.onDeactivate();
                fp.onActivate();
            } else {
                fp.toggle();
            }
            return SINGLE_SUCCESS;
        }));

        // Comando .fp despawn
        builder.then(literal("despawn").executes(context -> {
            FakePlayer fp = Modules.get().get(FakePlayer.class);
            if (fp.isActive()) fp.toggle();
            return SINGLE_SUCCESS;
        }));
    }
}