package dev.ozozorz.enhancementorderhints.render;

import dev.ozozorz.enhancementorderhints.config.ConfigManager;
import dev.ozozorz.enhancementorderhints.config.EnchantPlan;
import dev.ozozorz.enhancementorderhints.enchant.ItemCategory;
import dev.ozozorz.enhancementorderhints.optimizer.EnchantOrderOptimizer;
import dev.ozozorz.enhancementorderhints.optimizer.OptimizationResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HintPanelRenderer {
    private static String cacheKey;
    private static OptimizationResult cachedResult;

    private HintPanelRenderer() {}

    public static void render(AnvilScreen screen, GuiGraphicsExtractor graphics, int anvilLeft, int y,
                              int anvilWidth, int anvilHeight) {
        if (!ConfigManager.get().showPanel) return;
        Optional<EnchantPlan> planOptional = applicablePlan(screen);
        if (planOptional.isEmpty()) return;
        EnchantPlan plan = planOptional.get();
        PanelLayout.Bounds bounds = PanelLayout.find(screen.width, anvilLeft, anvilWidth);
        if (bounds == null) return;
        int x = bounds.x();
        int panelWidth = bounds.width();

        Font font = Minecraft.getInstance().font;
        OptimizationResult result = optimizeCached(plan);
        List<RenderedStep> renderedSteps = result.possible()
                ? createRenderedSteps(result.steps(), font, panelWidth - 14)
                : List.of();
        int desiredHeight = result.possible()
                ? 60 + renderedSteps.stream().mapToInt(RenderedStep::height).sum()
                : 70;
        PanelLayout.VerticalBounds vertical = PanelLayout.findVertical(screen.height, y, anvilHeight, desiredHeight);
        if (vertical == null) return;
        int panelY = vertical.y();
        int height = vertical.height();

        graphics.fill(x, panelY, x + panelWidth, panelY + height, 0xE6101010);
        graphics.outline(x, panelY, panelWidth, height, 0xFF8B8B8B);
        graphics.fill(x + 1, panelY + 1, x + panelWidth - 1, panelY + 20, 0xFF3C2F4F);
        drawTrimmed(graphics, font, Component.literal(plan.name), x + 7, panelY + 7, panelWidth - 14, 0xFFFFFFFF);

        int textY = panelY + 27;
        if (!result.possible()) {
            graphics.text(font, Component.translatable("panel.enhancement-order-hints.impossible"), x + 7, textY, 0xFFFF5555, false);
            graphics.text(font, Component.translatable("panel.enhancement-order-hints.open_config"), x + 7, textY + 13, 0xFFAAAAAA, false);
            return;
        }

        Component summary = Component.translatable("panel.enhancement-order-hints.summary", result.totalLevels());
        drawTrimmed(graphics, font, summary, x + 7, textY, panelWidth - 14, 0xFFFFD76A);
        textY += 15;
        graphics.text(font, Component.translatable("panel.enhancement-order-hints.steps"), x + 7, textY, 0xFFFFFFFF, true);
        textY += 13;

        boolean truncated = desiredHeight > height;
        int contentBottom = panelY + height - (truncated ? 15 : 5);
        int visible = 0;
        for (RenderedStep step : renderedSteps) {
            if (textY + step.height() > contentBottom) break;
            for (String line : step.titleLines()) {
                graphics.text(font, line, x + 7, textY, 0xFFE0E0E0, false);
                textY += 10;
            }
            graphics.text(font, step.cost(), x + 16, textY, 0xFF80D880, false);
            textY += 13;
            visible++;
        }
        if (renderedSteps.size() > visible) {
            graphics.text(font, Component.translatable("panel.enhancement-order-hints.more", renderedSteps.size() - visible),
                    x + panelWidth - 72, panelY + height - 12, 0xFFAAAAAA, false);
        }
    }

    private static Optional<EnchantPlan> applicablePlan(AnvilScreen screen) {
        ItemStack input = screen.getMenu().getSlot(0).getItem();
        if (!input.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
            Optional<ItemCategory> category = ItemCategory.detectItemId(itemId);
            if (category.isPresent()) {
                Optional<EnchantPlan> matching = ConfigManager.get().plans.stream()
                        .filter(plan -> category.get().id().equals(plan.targetItem)).findFirst();
                if (matching.isPresent()) return matching;
            }
        }
        return ConfigManager.selectedPlan();
    }

    private static OptimizationResult optimizeCached(EnchantPlan plan) {
        String key = plan.id + '|' + plan.optimizationMode + '|' + plan.enchantments.stream()
                .map(value -> value.id + ':' + value.level).reduce("", (a, b) -> a + ',' + b);
        if (!key.equals(cacheKey)) {
            cacheKey = key;
            cachedResult = EnchantOrderOptimizer.optimize(plan);
        }
        return cachedResult;
    }

    private static List<RenderedStep> createRenderedSteps(List<OptimizationResult.MergeStep> steps,
                                                           Font font, int maxWidth) {
        List<RenderedStep> rendered = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            OptimizationResult.MergeStep step = steps.get(index);
            Component title = Component.literal((index + 1) + ". ")
                    .append(localizedOperand(step.left()))
                    .append(" + ")
                    .append(localizedOperand(step.right()));
            List<String> lines = wrap(font, title, maxWidth);
            Component cost = Component.translatable("panel.enhancement-order-hints.step_cost", step.levelCost());
            rendered.add(new RenderedStep(lines, cost));
        }
        return rendered;
    }

    private static Component localizedOperand(OptimizationResult.Operand operand) {
        if (operand.isItem()) return Component.literal(operand.itemLabel());
        if (operand.enchantments().size() == 1) return localizedEnchantment(operand.enchantments().getFirst());

        Component contents = Component.empty();
        for (int index = 0; index < operand.enchantments().size(); index++) {
            if (index > 0) contents = contents.copy().append(Component.translatable("panel.enhancement-order-hints.list_separator"));
            contents = contents.copy().append(localizedEnchantment(operand.enchantments().get(index)));
        }
        return Component.translatable("panel.enhancement-order-hints.combined_book", contents);
    }

    private static Component localizedEnchantment(String value) {
        if (!value.startsWith("minecraft:")) return Component.literal(value);
        int levelSeparator = value.lastIndexOf(' ');
        String id = levelSeparator < 0 ? value : value.substring(0, levelSeparator);
        String level = levelSeparator < 0 ? "" : value.substring(levelSeparator);
        String path = id.substring("minecraft:".length());
        return Component.translatable("enchantment.minecraft." + path).append(level);
    }

    private static List<String> wrap(Font font, Component text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text.getString();
        while (!remaining.isEmpty()) {
            if (font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) break;
            int consumed = line.length();
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace > 3) {
                line = line.substring(0, lastSpace);
                consumed = lastSpace + 1;
            }
            lines.add(line.stripTrailing());
            remaining = remaining.substring(consumed).stripLeading();
        }
        if (lines.isEmpty()) lines.add("");
        return List.copyOf(lines);
    }

    private static void drawTrimmed(GuiGraphicsExtractor graphics, Font font, Component text,
                                    int x, int y, int maxWidth, int color) {
        String value = text.getString();
        if (font.width(value) > maxWidth) value = font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
        graphics.text(font, value, x, y, color, false);
    }

    private record RenderedStep(List<String> titleLines, Component cost) {
        private int height() {
            return titleLines.size() * 10 + 13;
        }
    }
}
