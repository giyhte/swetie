package dev.pulselite;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class PulseLiteClient implements ClientModInitializer {
    public static final String ID = "pulselite";
    public static KeyMapping OPEN_MENU;
    public static KeyMapping ZOOM;
    private static boolean zooming = false;
    private static int savedFov = 70;

    @Override
    public void onInitializeClient() {
        PulseConfig.load();
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        OPEN_MENU = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.pulselite.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        ZOOM = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.pulselite.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));
        ClientTickEvents.END_CLIENT_TICK.register(PulseLiteClient::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ID, "hud"), PulseHud::render);
    }

    private static void tick(Minecraft mc) {
        while (OPEN_MENU.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new PulseMenuScreen(null));
        }
        boolean want = PulseConfig.get().zoom && ZOOM.isDown() && mc.screen == null;
        if (want && !zooming) {
            savedFov = mc.options.fov().get();
            zooming = true;
        }
        if (want) {
            mc.options.fov().set(30);
        } else if (zooming) {
            mc.options.fov().set(savedFov);
            zooming = false;
        }
    }
}
