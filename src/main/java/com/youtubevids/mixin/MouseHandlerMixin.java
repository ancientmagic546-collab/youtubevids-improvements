package com.youtubevids.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.youtubevids.YouTubeVidsClient;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow @Final private Minecraft minecraft;

	@Inject(method = "onPress", at = @At("TAIL"))
	private void youtubevids$onMousePress(long window, int button, int action, int mods, CallbackInfo ci) {
		if (button != 0 || action != 1) return;
		if (YouTubeVidsClient.HANDLER == null || YouTubeVidsClient.CONFIG == null) return;
		if (!YouTubeVidsClient.CONFIG.isEnabled()) return;
		if (minecraft.screen != null) return;
		YouTubeVidsClient.HANDLER.onAttackClick(minecraft);
	}
}
