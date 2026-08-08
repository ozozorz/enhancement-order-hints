package dev.ozozorz.enhancementorderhints.mixin.client;

import dev.ozozorz.enhancementorderhints.render.HintPanelRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
    private static final int ANVIL_WIDTH = 176;
    private static final int ANVIL_HEIGHT = 166;

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void enhancementOrderHints$renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                    float delta, CallbackInfo callbackInfo) {
        AnvilScreen screen = (AnvilScreen) (Object) this;
        int left = (screen.width - ANVIL_WIDTH) / 2;
        int top = (screen.height - ANVIL_HEIGHT) / 2;
        HintPanelRenderer.render(screen, graphics, left, top, ANVIL_WIDTH, ANVIL_HEIGHT);
    }
}
