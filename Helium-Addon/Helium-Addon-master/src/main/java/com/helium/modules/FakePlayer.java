package com.helium.modules;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.enchantment.Enchantments;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class FakePlayer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> playerName = sgGeneral.add(new StringSetting.Builder()
            .name("nombre")
            .defaultValue("Helium")
            .build()
    );

    private final Setting<Double> initialHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("salud-inicial")
            .defaultValue(20.0)
            .min(1.0)
            .sliderMax(100.0)
            .build()
    );

    private OtherClientPlayerEntity fakePlayer;

    public FakePlayer() {
        super(HeliumAddon.CATEGORY, "fake-player", "Bot con gravedad, knockback y pops (Sin getPos).");
    }

    private ItemStack createNetherite(net.minecraft.item.Item item) {
        ItemStack stack = new ItemStack(item);
        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder enchants = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        enchants.add(registry.getOrThrow(Enchantments.PROTECTION), 4);
        enchants.add(registry.getOrThrow(Enchantments.UNBREAKING), 3);

        stack.set(DataComponentTypes.ENCHANTMENTS, enchants.build());
        return stack;
    }

    @Override
    public void onActivate() {
        if (mc.world == null || mc.player == null) {
            this.toggle();
            return;
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), playerName.get());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);

        // Copiamos posición usando los métodos directos
        fakePlayer.refreshPositionAndAngles(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch());
        fakePlayer.headYaw = mc.player.headYaw;
        fakePlayer.setHealth(initialHealth.get().floatValue());

        // Equipamiento base
        fakePlayer.equipStack(EquipmentSlot.HEAD, createNetherite(Items.NETHERITE_HELMET));
        fakePlayer.equipStack(EquipmentSlot.CHEST, createNetherite(Items.NETHERITE_CHESTPLATE));
        fakePlayer.equipStack(EquipmentSlot.LEGS, createNetherite(Items.NETHERITE_LEGGINGS));
        fakePlayer.equipStack(EquipmentSlot.FEET, createNetherite(Items.NETHERITE_BOOTS));
        fakePlayer.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        mc.world.addEntity(fakePlayer);
        ChatUtils.info("Fake Player '" + playerName.get() + "' spawneado.");
    }

    @Override
    public void onDeactivate() {
        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (fakePlayer == null) return;

        // Gravedad: tickMovement procesa la aceleración de caída
        fakePlayer.tick();
        fakePlayer.tickMovement();

        // Reponer tótem
        if (fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            fakePlayer.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        if (fakePlayer.getHealth() <= 0) handlePop();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (fakePlayer == null || event.entity.getId() != fakePlayer.getId()) return;

        // Calcular retroceso sin getPos()
        double diffX = fakePlayer.getX() - mc.player.getX();
        double diffZ = fakePlayer.getZ() - mc.player.getZ();

        // Normalizar vector de empuje manual
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        if (distance > 0) {
            fakePlayer.addVelocity((diffX / distance) * 0.4, 0.25, (diffZ / distance) * 0.4);
        }

        // Aplicar daño
        float damage = 5.0f;
        if (mc.player.getMainHandStack().getItem() == Items.MACE && mc.player.fallDistance > 1.2f) {
            damage += (mc.player.fallDistance * 2.0f);
        }

        fakePlayer.setHealth(fakePlayer.getHealth() - damage);
        fakePlayer.handleStatus((byte) 2); // Animación roja
    }

    private void handlePop() {
        mc.world.sendEntityStatus(fakePlayer, (byte) 35); // Pop visual
        fakePlayer.setHealth(2.0f);
        fakePlayer.clearStatusEffects();
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        ChatUtils.info("¡POP!");
    }
}