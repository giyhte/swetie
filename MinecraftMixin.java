package dev.pulselite.mixin;

import dev.pulselite.PulseConfig;
import dev.pulselite.PulseTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /** Подменяет ванильное главное меню на градиентное PulseLite. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen pulselite$swapTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class && PulseConfig.get().customTitle) {
            return new PulseTitleScreen();
        }
        return screen;
    }
}
