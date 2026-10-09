package com.youtubevids.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import com.youtubevids.YouTubeVidsMod;

public final class ModConfig {
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("youtubevids.properties");

	private boolean enabled = false;
	private boolean emptyHandToSpear = true;
	private boolean windChargeToSpear = true;
	private boolean swordToMace = true;
	private boolean spearToMace = true;
	private boolean returnToPreviousSlot = true;
	private int actionDelayTicks = 1;
	/** Hotbar index 0-8. Default 1 = 2nd slot (spear). */
	private int preferredSpearSlot = 1;
	/** Hotbar index 0-8. Default 2 = 3rd slot (mace). */
	private int preferredMaceSlot = 2;

	public void load() {
		if (!Files.exists(CONFIG_PATH)) {
			save();
			return;
		}
		Properties props = new Properties();
		try (var in = Files.newInputStream(CONFIG_PATH)) {
			props.load(in);
			enabled = Boolean.parseBoolean(props.getProperty("enabled", "false"));
			emptyHandToSpear = Boolean.parseBoolean(props.getProperty("emptyHandToSpear", "true"));
			windChargeToSpear = Boolean.parseBoolean(props.getProperty("windChargeToSpear", "true"));
			swordToMace = Boolean.parseBoolean(props.getProperty("swordToMace", "true"));
			spearToMace = Boolean.parseBoolean(props.getProperty("spearToMace", "true"));
			returnToPreviousSlot = Boolean.parseBoolean(props.getProperty("returnToPreviousSlot", "true"));
			actionDelayTicks = Math.max(0, Integer.parseInt(props.getProperty("actionDelayTicks", "1")));
			preferredSpearSlot = Integer.parseInt(props.getProperty("preferredSpearSlot", "1"));
			preferredMaceSlot = Integer.parseInt(props.getProperty("preferredMaceSlot", "2"));
		} catch (Exception e) {
			YouTubeVidsMod.LOGGER.warn("Failed to load config, using defaults", e);
		}
	}

	public void save() {
		Properties props = new Properties();
		props.setProperty("enabled", String.valueOf(enabled));
		props.setProperty("emptyHandToSpear", String.valueOf(emptyHandToSpear));
		props.setProperty("windChargeToSpear", String.valueOf(windChargeToSpear));
		props.setProperty("swordToMace", String.valueOf(swordToMace));
		props.setProperty("spearToMace", String.valueOf(spearToMace));
		props.setProperty("returnToPreviousSlot", String.valueOf(returnToPreviousSlot));
		props.setProperty("actionDelayTicks", String.valueOf(actionDelayTicks));
		props.setProperty("preferredSpearSlot", String.valueOf(preferredSpearSlot));
		props.setProperty("preferredMaceSlot", String.valueOf(preferredMaceSlot));
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (var out = Files.newOutputStream(CONFIG_PATH)) {
				props.store(out, "YouTube Vids Improvements - Client Config");
			}
		} catch (IOException e) {
			YouTubeVidsMod.LOGGER.warn("Failed to save config", e);
		}
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean v) { enabled = v; save(); }
	public void toggleEnabled() { setEnabled(!enabled); }
	public boolean isEmptyHandToSpear() { return emptyHandToSpear; }
	public boolean isWindChargeToSpear() { return windChargeToSpear; }
	public boolean isSwordToMace() { return swordToMace; }
	public boolean isSpearToMace() { return spearToMace; }
	public boolean isReturnToPreviousSlot() { return returnToPreviousSlot; }
	public int getActionDelayTicks() { return actionDelayTicks; }
	public int getPreferredSpearSlot() { return preferredSpearSlot; }
	public int getPreferredMaceSlot() { return preferredMaceSlot; }
}
