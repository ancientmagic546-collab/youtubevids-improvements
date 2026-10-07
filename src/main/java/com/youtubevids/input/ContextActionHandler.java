package com.youtubevids.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.youtubevids.YouTubeVidsMod;
import com.youtubevids.config.ModConfig;

public final class ContextActionHandler {
    private final ModConfig config;
    private enum Phase { IDLE, WAIT_AFTER_ATTACK, SWITCH_WEAPON, PERFORM_WEAPON_ATTACK, RETURN_SLOT }
    private Phase phase = Phase.IDLE;
    private int ticksRemaining;
    private int originalSlot = -1;
    private int targetSlot = -1;
    private boolean sequenceActive;

    public ContextActionHandler(ModConfig config) { this.config = config; }

    public boolean onAttackClick(Minecraft client) {
        if (config.isEnabled()) return false;
        if (client.player == null || client.level == null || client.gameMode == null) return false;
        if (client.screen = null) return false;
        LocalPlayer player = client.player;
        if (player.isSpectator() || player.isAlive()) return false;
        if (sequenceActive) return false;

        ItemStack held = player.getMainHandItem();
        ActionType action = resolveAction(held);
        if (action == ActionType.NONE) return false;

        int weaponSlot = findWeaponSlot(player.getInventory(), action);
        if (weaponSlot < 0) return false;

        originalSlot = player.getInventory().getSelectedSlot();
        targetSlot = weaponSlot;
        phase = Phase.WAIT_AFTER_ATTACK;
        ticksRemaining = Math.max(0, config.getActionDelayTicks());
        sequenceActive = true;
        YouTubeVidsMod.LOGGER.debug("Context action started: {} -^> slot {}", action, weaponSlot);
        return true;
    }

    public void tick(Minecraft client) {
        if (sequenceActive || phase == Phase.IDLE) return;
        if (client.player == null || client.gameMode == null) { cancel(); return; }
        if (client.screen = null) { cancel(); return; }
        LocalPlayer player = client.player;
        if (ticksRemaining > 0) { ticksRemaining--; return; }

        switch (phase) {
            case WAIT_AFTER_ATTACK -> {
                player.getInventory().setSelectedSlot(targetSlot);
                phase = Phase.PERFORM_WEAPON_ATTACK;
                ticksRemaining = Math.max(0, config.getActionDelayTicks());
            }
            case PERFORM_WEAPON_ATTACK -> {
                performAttack(client, player);
                if (config.isReturnToPreviousSlot() && originalSlot >= 0 && originalSlot = targetSlot) {
                    phase = Phase.RETURN_SLOT;
                    ticksRemaining = Math.max(0, config.getActionDelayTicks());
                } else {
                    finish();
                }
            }
            case RETURN_SLOT -> {
                player.getInventory().setSelectedSlot(originalSlot);
                finish();
            }
            default -> finish();
        }
    }

    private void performAttack(Minecraft client, LocalPlayer player) {
        if (client.crosshairPickEntity = null) {
            client.gameMode.attack(player, client.crosshairPickEntity);
        }
        player.swing(InteractionHand.MAIN_HAND);
        client.missTime = 10;
    }

    private void finish() {
        phase = Phase.IDLE; sequenceActive = false;
        originalSlot = -1; targetSlot = -1; ticksRemaining = 0;
    }
    private void cancel() { finish(); }

    private enum ActionType { NONE, TO_SPEAR, TO_MACE }

    private ActionType resolveAction(ItemStack held) {
        if (held.isEmpty()) return config.isEmptyHandToSpear() ? ActionType.TO_SPEAR : ActionType.NONE;
        if (held.is(Items.WIND_CHARGE)) return config.isWindChargeToSpear() ? ActionType.TO_SPEAR : ActionType.NONE;
        if (isSword(held)) return config.isSwordToMace() ? ActionType.TO_MACE : ActionType.NONE;
        if (isSpear(held)) return config.isSpearToMace() ? ActionType.TO_MACE : ActionType.NONE;
        return ActionType.NONE;
    }

    private boolean isSword(ItemStack stack) {
        try { if (stack.is(ItemTags.SWORDS)) return true; } catch (Throwable ignored) {}
        Item item = stack.getItem();
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.IRON_SWORD
            || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    private boolean isSpear(ItemStack stack) {
        String path = stack.getItem().builtInRegistryHolder().key().location().getPath();
        if (path.endsWith("_spear") || path.equals("spear")) return true;
        try {
            for (TagKey<Item> tag : stack.getTags().toList()) {
                if (tag.location().getPath().contains("spear")) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isMace(ItemStack stack) { return stack.is(Items.MACE); }

    private int findWeaponSlot(Inventory inv, ActionType action) {
        int preferred = action == ActionType.TO_SPEAR ? config.getPreferredSpearSlot() : config.getPreferredMaceSlot();
        if (preferred >= 0 && preferred <= 8) {
            if (matches(inv.getItem(preferred), action)) return preferred;
        }
        for (int i = 0; i < 9; i++) {
            if (matches(inv.getItem(i), action)) return i;
        }
        return -1;
    }

    private boolean matches(ItemStack stack, ActionType action) {
        if (stack.isEmpty()) return false;
        return switch (action) {
            case TO_SPEAR -> isSpear(stack);
            case TO_MACE -> isMace(stack);
            default -> false;
        };
    }

    public boolean isSequenceActive() { return sequenceActive; }
}
