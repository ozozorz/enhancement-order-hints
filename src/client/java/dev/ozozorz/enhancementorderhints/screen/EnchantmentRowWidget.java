package dev.ozozorz.enhancementorderhints.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class EnchantmentRowWidget extends AbstractWidget {
    private final int color;

    EnchantmentRowWidget(int x, int y, int width, int height, Component label, int color) {
        super(x, y, width, height, label);
        this.color = color;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
        graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 7, getY() + 7, 0xFFF2F2F2, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
