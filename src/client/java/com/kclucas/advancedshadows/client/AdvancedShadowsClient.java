package com.kclucas.advancedshadows.client;

import com.kclucas.advancedshadows.AdvancedShadows;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AdvancedShadowsClient implements ClientModInitializer {

	public static boolean overlayEnabled = false;
	private static KeyBinding toggleKey;

	@Override
	public void onInitializeClient() {
		// Keybind: F7 (wie Light Overlay) zum Togglen
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.advancedshadows.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                KeyBinding.Category.MISC
        ));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				overlayEnabled = !overlayEnabled;
				if (client.player != null) {
					client.player.sendMessage(
							net.minecraft.text.Text.literal(
									overlayEnabled
											? "§aShadow Overlay: ON"
											: "§cShadow Overlay: OFF"
							),
							true // action bar statt chat
					);
				}
			}
		});

		// Render-Event einhängen
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			if (overlayEnabled) {
				ShadowOverlayRenderer.render(context);
			}
		});

		AdvancedShadows.LOGGER.info("AdvancedShadows client initialized. Press F7 to toggle.");
	}
}