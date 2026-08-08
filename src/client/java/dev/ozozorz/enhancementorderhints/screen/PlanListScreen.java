package dev.ozozorz.enhancementorderhints.screen;

import dev.ozozorz.enhancementorderhints.config.ConfigManager;
import dev.ozozorz.enhancementorderhints.config.EnchantPlan;
import dev.ozozorz.enhancementorderhints.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public final class PlanListScreen extends Screen {
    private static final int MAX_ROWS = 6;
    private final Screen parent;
    private int page;

    public PlanListScreen(Screen parent) {
        super(Component.translatable("screen.enhancement-order-hints.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(500, width - 30);
        int x = (width - panelWidth) / 2;
        int firstY = 50;
        ModConfig config = ConfigManager.get();
        int rows = visibleRows();
        int start = page * rows;

        for (int row = 0; row < rows && start + row < config.plans.size(); row++) {
            EnchantPlan plan = config.plans.get(start + row);
            int y = firstY + row * 28;
            boolean active = plan.id.equals(config.selectedPlanId);
            Component selectText = Component.literal((active ? "● " : "○ ") + plan.name);
            addRenderableWidget(Button.builder(selectText, button -> {
                config.selectedPlanId = plan.id;
                ConfigManager.save();
                rebuildWidgets();
            }).bounds(x, y, panelWidth - 155, 22).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.enhancement-order-hints.edit"), button ->
                    minecraft.setScreen(new PlanEditorScreen(this, plan, false)))
                    .bounds(x + panelWidth - 150, y, 70, 22).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.enhancement-order-hints.delete"), button -> {
                config.plans.removeIf(value -> value.id.equals(plan.id));
                if (plan.id.equals(config.selectedPlanId)) {
                    config.selectedPlanId = config.plans.isEmpty() ? null : config.plans.getFirst().id;
                }
                ConfigManager.save();
                int maxPage = Math.max(0, (config.plans.size() - 1) / rows);
                page = Math.min(page, maxPage);
                rebuildWidgets();
            }).bounds(x + panelWidth - 75, y, 75, 22).build());
        }

        int bottom = firstY + rows * 28 + 8;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            rebuildWidgets();
        }).bounds(x, bottom, 30, 20).build()).active = page > 0;
        int maxPage = Math.max(0, (config.plans.size() - 1) / rows);
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min(maxPage, page + 1);
            rebuildWidgets();
        }).bounds(x + 35, bottom, 30, 20).build()).active = page < maxPage;
        addRenderableWidget(Button.builder(Component.translatable("gui.enhancement-order-hints.add"), button -> {
            EnchantPlan plan = new EnchantPlan();
            plan.enchantments = new ArrayList<>();
            minecraft.setScreen(new PlanEditorScreen(this, plan, true));
        }).bounds(x + panelWidth - 150, bottom, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.enhancement-order-hints.toggle_panel",
                config.showPanel ? Component.translatable("options.on") : Component.translatable("options.off")), button -> {
            config.showPanel = !config.showPanel;
            ConfigManager.save();
            rebuildWidgets();
        }).bounds(x, height - 30, 210, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x + panelWidth - 100, height - 30, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, title, width / 2 - font.width(title) / 2, 18, 0xFFFFFFFF, true);
        if (ConfigManager.get().plans.isEmpty()) {
            Component empty = Component.translatable("gui.enhancement-order-hints.empty");
            graphics.text(font, empty, width / 2 - font.width(empty) / 2, 80, 0xFFAAAAAA, false);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private int visibleRows() {
        return Math.max(3, Math.min(MAX_ROWS, (height - 50 - 68) / 28));
    }
}
