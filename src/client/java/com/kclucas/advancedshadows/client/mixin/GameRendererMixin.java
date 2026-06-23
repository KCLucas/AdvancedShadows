package com.kclucas.advancedshadows.client.mixin;

import com.kclucas.advancedshadows.client.render.ShadowOverlayRenderer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "close", at = @At("RETURN"))
	private void onClose(CallbackInfo ci) {
		if (ShadowOverlayRenderer.getInstance() != null) {
			ShadowOverlayRenderer.getInstance().close();
		}
	}
}