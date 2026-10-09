package com.youtubevids.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
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

	private enum Phase {
		IDLE,
		WAIT_AFTER_ATTACK,
		PERFORM_WEAPON_ATTACK,
		RETURN_SLOT
	}

	private Phase phase = Phase.IDLE;
	private int ticksRemaining;
	private int originalSlot = -1;
	private int targetSlot = -1;
	private boolean sequenceActive;

	public ContextActionHandler(ModConfig config) {
		this.config = config;
	}

	public boolean onAttackClick(Minecraft client) {
		if (!config.isEnabled()) return false;
		if (client.player == null || client.level == null || client.gameMode == null) return false;
		if (client.screen != null) return false;

		LocalPlayer player = client.player;
		if (player.isSpectator() || !player.isAlive()) return false;
		if (sequenceActive) return false;

		ItemStack held = player.getMainHandItem();
		ActionType action = resolveAction(held);
		if (action == ActionType.NONE) return false;

		int weaponSlot = findWeaponSlot(player.getInventory(), action);
		if (weaponSlot < 0) {
			YouTubeVidsMod.LOGGER.info("Context action skipped: no weapon slot for {}", action);
			return false;
		}

		originalSlot = player.getInventory().getSelectedSlot();
		targetSlot = weaponSlot;
		phase = Phase.WAIT_AFTER_ATTACK;
		ticksRemaining = Math.max(0, config.getActionDelayTicks());
		sequenceActive = true;
		YouTubeVidsMod.LOGGER.info("Context action: {} held, switch to slot {} then attack", action, weaponSlot + 1);
		return true;
	}

	public void tick(Minecraft client) {
		if (!sequenceActive || phase == Phase.IDLE) return;
		if (client.player == null || client.gameMode == null) {
			cancel();
			return;
		}
		if (client.screen != null) {
			cancel();
			return;
		}

		LocalPlayer player = client.player;
		if (ticksRemaining > 0) {
			ticksRemaining--;
			return;
		}

		switch (phase) {
			case WAIT_AFTER_ATTACK -> {
				selectHotbarSlot(client, player, targetSlot);
				phase = Phase.PERFORM_WEAPON_ATTACK;
				ticksRemaining = Math.max(0, config.getActionDelayTicks());
			}
			case PERFORM_WEAPON_ATTACK -> {
				performAttack(client, player);
				if (config.isReturnToPreviousSlot() && originalSlot >= 0 && originalSlot != targetSlot) {
					phase = Phase.RETURN_SLOT;
					ticksRemaining = Math.max(0, config.getActionDelayTicks());
				} else {
					finish();
				}
			}
			case RETURN_SLOT -> {
				selectHotbarSlot(client, player, originalSlot);
				finish();
			}
			default -> finish();
		}
	}

	/** Change hotbar slot on client AND tell the server. */
	private void selectHotbarSlot(Minecraft client, LocalPlayer player, int slot) {
		if (slot < 0 || slot > 8) return;
		player.getInventory().setSelectedSlot(slot);
		// Critical: server must know the new selected slot
		if (client.getConnection() != null) {
			client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
		}
	}

	private void performAttack(Minecraft client, LocalPlayer player) {
		if (client.crosshairPickEntity != null) {
			client.gameMode.attack(player, client.crosshairPickEntity);
		}
		player.swing(InteractionHand.MAIN_HAND);
		client.missTime = 10;
	}

	private void finish() {
		phase = Phase.IDLE;
		sequenceActive = false;
		originalSlot = -1;
		targetSlot = -1;
		ticksRemaining = 0;
	}

	private void cancel() {
		finish();
	}

	private enum ActionType {
		NONE,
		TO_SPEAR,
		TO_MACE
	}

	private ActionType resolveAction(ItemStack held) {
		if (held.isEmpty()) {
			return config.isEmptyHandToSpear() ? ActionType.TO_SPEAR : ActionType.NONE;
		}
		if (isWindCharge(held)) {
			return config.isWindChargeToSpear() ? ActionType.TO_SPEAR : ActionType.NONE;
		}
		if (isSword(held)) {
			return config.isSwordToMace() ? ActionType.TO_MACE : ActionType.NONE;
		}
		if (isSpear(held)) {
			return config.isSpearToMace() ? ActionType.TO_MACE : ActionType.NONE;
		}
		return ActionType.NONE;
	}

	private boolean isWindCharge(ItemStack stack) {
		if (stack.is(Items.WIND_CHARGE)) return true;
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getPath().equals("wind_charge");
	}

	private boolean isSword(ItemStack stack) {
		try {
			if (stack.is(ItemTags.SWORDS)) return true;
		} catch (Throwable ignored) {
		}
		Item item = stack.getItem();
		return item == Items.WOODEN_SWORD
				|| item == Items.STONE_SWORD
				|| item == Items.IRON_SWORD
				|| item == Items.GOLDEN_SWORD
				|| item == Items.DIAMOND_SWORD
				|| item == Items.NETHERITE_SWORD
				|| pathEndsWith(stack, "_sword");
	}

	private boolean isSpear(ItemStack stack) {
		if (pathEndsWith(stack, "_spear") || pathEquals(stack, "spear")) return true;
		try {
			for (TagKey<Item> tag : stack.getTags().toList()) {
				if (tag.location().getPath().contains("spear")) return true;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private boolean isMace(ItemStack stack) {
		if (stack.is(Items.MACE)) return true;
		return pathEquals(stack, "mace");
	}

	private boolean pathEndsWith(ItemStack stack, String suffix) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getPath().endsWith(suffix);
	}

	private boolean pathEquals(ItemStack stack, String path) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getPath().equals(path);
	}

	/**
	 * Defaults: spear = hotbar slot 2 (index 1), mace = hotbar slot 3 (index 2).
	 * Uses preferred slot from config if set; otherwise searches hotbar.
	 */
	private int findWeaponSlot(Inventory inv, ActionType action) {
		int preferred = action == ActionType.TO_SPEAR
				? config.getPreferredSpearSlot()
				: config.getPreferredMaceSlot();

		// Your layout: slot 2 = spear, slot 3 = mace (1-based UI → 0-based index)
		if (preferred < 0) {
			preferred = action == ActionType.TO_SPEAR ? 1 : 2;
		}

		if (preferred >= 0 && preferred <= 8) {
			ItemStack stack = inv.getItem(preferred);
			// Prefer configured slot even if empty check fails — user layout is trusted
			if (!stack.isEmpty() && matches(stack, action)) {
				return preferred;
			}
			// Still use preferred slot if user put the weapon there (trust layout)
			if (!stack.isEmpty()) {
				return preferred;
			}
		}

		for (int i = 0; i < 9; i++) {
			if (matches(inv.getItem(i), action)) {
				return i;
			}
		}
		return preferred >= 0 && preferred <= 8 ? preferred : -1;
	}

	private boolean matches(ItemStack stack, ActionType action) {
		if (stack.isEmpty()) return false;
		return switch (action) {
			case TO_SPEAR -> isSpear(stack);
			case TO_MACE -> isMace(stack);
			default -> false;
		};
	}

	public boolean isSequenceActive() {
		return sequenceActive;
	}
}
