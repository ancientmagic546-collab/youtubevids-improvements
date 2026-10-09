package com.youtubevids;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.youtubevids.config.ModConfig;
import com.youtubevids.input.ContextActionHandler;

public class YouTubeVidsClient implements ClientModInitializer {
	public static ModConfig CONFIG;
	public static ContextActionHandler HANDLER;
	public static KeyMapping TOGGLE_KEY;

	/** Previous tick attack-key state for rising-edge detection */
	private boolean wasAttackDown = false;

	@Override
	public void onInitializeClient() {
		CONFIG = new ModConfig();
		CONFIG.load();
		HANDLER = new ContextActionHandler(CONFIG);

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(YouTubeVidsMod.MOD_ID, "youtubevids")
		);

		TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.youtubevids.context_action_toggle",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_G,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		YouTubeVidsMod.LOGGER.info("YouTube Vids Improvements initialized (context actions default: {})",
				CONFIG.isEnabled() ? "ON" : "OFF");
	}

	private void onClientTick(Minecraft client) {
		// Toggle key
		while (TOGGLE_KEY.consumeClick()) {
			CONFIG.toggleEnabled();
			boolean on = CONFIG.isEnabled();
			if (client.player != null) {
				client.player.displayClientMessage(
						Component.translatable(on
								? "youtubevids.context_actions.on"
								: "youtubevids.context_actions.off"),
						true
				);
			}
			YouTubeVidsMod.LOGGER.info("Context Actions toggled: {}", on ? "ON" : "OFF");
		}

		// Rising edge of attack (left click) — does not consume the vanilla click
		boolean attackDown = client.options.keyAttack.isDown();
		if (attackDown && !wasAttackDown
				&& CONFIG != null && CONFIG.isEnabled()
				&& HANDLER != null
				&& client.player != null
				&& client.screen == null) {
			HANDLER.onAttackClick(client);
		}
		wasAttackDown = attackDown;

		if (HANDLER != null) {
			HANDLER.tick(client);
		}
	}
}
