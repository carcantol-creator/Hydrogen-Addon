package com.hydrogen.dyd.modules;

import com.hydrogen.dyd.HeliumMaster;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.registry.RegistryKeys;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class FakePlayer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> playerName = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .defaultValue("Donalp012")
        .build()
    );

    private final Setting<Double> initialHealth = sgGeneral.add(new DoubleSetting.Builder()
        .name("salud-inicial")
        .defaultValue(20.0)
        .min(1.0)
        .sliderMax(100.0)
        .build()
    );

    private final Setting<Integer> failChance = sgGeneral.add(new IntSetting.Builder()
        .name("totem-fail-%")
        .defaultValue(15)
        .min(0)
        .max(100)
        .build()
    );

    private OtherClientPlayerEntity fakePlayer;

    public FakePlayer() {
        super(HeliumMaster.CATEGORY, "Fake Player", "Fake Player for prove configs.");
    }

    private ItemStack createNetherite(net.minecraft.item.Item item) {
        ItemStack stack = new ItemStack(item);
        ItemEnchantmentsComponent.Builder enchants = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        enchants.add(registry.getOrThrow(Enchantments.PROTECTION), 4);
        enchants.add(registry.getOrThrow(Enchantments.UNBREAKING), 3);
        enchants.add(registry.getOrThrow(Enchantments.MENDING), 1);

        stack.set(DataComponentTypes.ENCHANTMENTS, enchants.build());
        return stack;
    }

    public void spawn() {
        if (mc.world == null || mc.player == null) return;
        if (fakePlayer != null) despawn();

        GameProfile profile = new GameProfile(UUID.randomUUID(), playerName.get());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);

        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHealth(initialHealth.get().floatValue());

        fakePlayer.equipStack(EquipmentSlot.HEAD, createNetherite(Items.NETHERITE_HELMET));
        fakePlayer.equipStack(EquipmentSlot.CHEST, createNetherite(Items.NETHERITE_CHESTPLATE));
        fakePlayer.equipStack(EquipmentSlot.LEGS, createNetherite(Items.NETHERITE_LEGGINGS));
        fakePlayer.equipStack(EquipmentSlot.FEET, createNetherite(Items.NETHERITE_BOOTS));

        fakePlayer.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        mc.world.addEntity(fakePlayer);
        ChatUtils.info("Fake Player '" + playerName.get() + "' spawneado.");
    }

    public void despawn() {
        if (fakePlayer != null && mc.world != null) {
            fakePlayer.discard();
            fakePlayer = null;
            ChatUtils.info("Fake Player eliminado.");
        }
    }

    @Override
    public void onActivate() { spawn(); }

    @Override
    public void onDeactivate() { despawn(); }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (fakePlayer == null || !fakePlayer.isAlive()) return;

        if (fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            fakePlayer.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        if (fakePlayer.getHealth() <= 0) {
            if (Math.random() * 100 < failChance.get()) {
                ChatUtils.warning("¡TOTEM FAIL!");
                fakePlayer.discard();
            } else {
                handlePop();
            }
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (fakePlayer == null || event.entity.getId() != fakePlayer.getId()) return;

        float damage = 4.0f;

        if (mc.player.getMainHandStack().getItem() == Items.MACE && mc.player.fallDistance > 1.5f) {
            float maceBonus = (float) (mc.player.fallDistance * 2.5);
            damage += maceBonus;
            ChatUtils.info("Impacto de Maza: +" + String.format("%.2f", maceBonus));
        }

        fakePlayer.setHealth(fakePlayer.getHealth() - damage);
        fakePlayer.handleStatus((byte) 2);
    }

    private void handlePop() {
        fakePlayer.handleStatus((byte) 35);
        fakePlayer.setHealth(2.0f);
        fakePlayer.clearStatusEffects();
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        ChatUtils.info("¡POP!");
    }
}
