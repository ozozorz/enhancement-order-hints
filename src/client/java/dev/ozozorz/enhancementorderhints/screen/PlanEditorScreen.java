package dev.ozozorz.enhancementorderhints.screen;

import dev.ozozorz.enhancementorderhints.config.ConfigManager;
import dev.ozozorz.enhancementorderhints.config.EnchantPlan;
import dev.ozozorz.enhancementorderhints.config.EnchantmentSelection;
import dev.ozozorz.enhancementorderhints.config.OptimizationMode;
import dev.ozozorz.enhancementorderhints.enchant.EnchantmentCatalog;
import dev.ozozorz.enhancementorderhints.enchant.EnchantmentInfo;
import dev.ozozorz.enhancementorderhints.enchant.ItemCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PlanEditorScreen extends Screen {
    private static final int MAX_ROWS = 8;
    private static final int ROW_START_Y = 112;
    private static final int ROW_HEIGHT = 24;
    private static final int[] REGULAR_BANDS = {0xD0232730, 0xD02D313B};
    private static final int[] CONFLICT_BANDS = {0xD0442F4E, 0xD0343F44, 0xD0443B2F};

    private final Screen parent;
    private final EnchantPlan working;
    private final boolean newPlan;
    private EditBox nameBox;
    private int page;
    private int dropdownPage;
    private boolean dropdownOpen;
    private Component notice = Component.empty();

    public PlanEditorScreen(Screen parent, EnchantPlan plan, boolean newPlan) {
        super(Component.translatable(newPlan ? "gui.enhancement-order-hints.add" : "gui.enhancement-order-hints.edit"));
        this.parent = parent;
        this.working = plan.copy();
        this.working.targetItem = ItemCategory.fromStoredValue(this.working.targetItem).id();
        this.newPlan = newPlan;
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        int x = panelX();
        nameBox = new EditBox(font, x + 90, 32, panelWidth - 90, 20,
                Component.translatable("gui.enhancement-order-hints.name"));
        nameBox.setMaxLength(60);
        nameBox.setValue(working.name);
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(categoryLabel(), button -> {
            preserveInputs();
            dropdownOpen = !dropdownOpen;
            rebuildWidgets();
        }).bounds(x + 90, 58, panelWidth - 90, 20).build());

        if (dropdownOpen) buildDropdown(x, panelWidth);
        else buildEnchantments(x, panelWidth);
        buildFooter(x, panelWidth);
    }

    private void buildDropdown(int x, int panelWidth) {
        ItemCategory[] categories = ItemCategory.values();
        int rows = dropdownRows();
        int start = dropdownPage * rows;
        for (int row = 0; row < rows && start + row < categories.length; row++) {
            ItemCategory category = categories[start + row];
            Component label = Component.literal(category == currentCategory() ? "● " : "  ")
                    .append(Component.translatable(category.translationKey()));
            addRenderableWidget(Button.builder(label, button -> selectCategory(category))
                    .bounds(x + 90, 82 + row * 22, panelWidth - 90, 20).build());
        }
        int navY = 82 + rows * 22 + 2;
        int maxPage = Math.max(0, (categories.length - 1) / rows);
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            dropdownPage = Math.max(0, dropdownPage - 1);
            rebuildWidgetsPreservingInputs();
        }).bounds(x + 90, navY, 30, 20).build());
        previous.active = dropdownPage > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            dropdownPage = Math.min(maxPage, dropdownPage + 1);
            rebuildWidgetsPreservingInputs();
        }).bounds(x + 125, navY, 30, 20).build());
        next.active = dropdownPage < maxPage;
        Component pageLabel = Component.translatable("gui.enhancement-order-hints.page", dropdownPage + 1, maxPage + 1);
        Button pageButton = addRenderableWidget(Button.builder(pageLabel, button -> {})
                .bounds(x + 160, navY, 90, 20).build());
        pageButton.active = false;
    }

    private void buildEnchantments(int x, int panelWidth) {
        addRenderableWidget(Button.builder(modeLabel(), button -> {
            working.optimizationMode = working.optimizationMode.next();
            button.setMessage(modeLabel());
        }).bounds(x, 84, panelWidth, 20).build());

        List<EnchantmentInfo> displayed = displayedEnchantments();
        int rows = visibleRows();
        int maxPage = Math.max(0, (displayed.size() - 1) / rows);
        page = Math.min(page, maxPage);
        int start = page * rows;
        int labelWidth = Math.min(150, Math.max(104, panelWidth - 200));
        for (int row = 0; row < rows && start + row < displayed.size(); row++) {
            EnchantmentInfo info = displayed.get(start + row);
            int y = ROW_START_Y + row * ROW_HEIGHT;
            int bandKey = EnchantmentCatalog.bandKey(info, displayed);
            boolean conflictGroup = EnchantmentCatalog.hasConflictGroup(info, displayed);
            int color = conflictGroup
                    ? CONFLICT_BANDS[Math.floorMod(bandKey, CONFLICT_BANDS.length)]
                    : REGULAR_BANDS[Math.floorMod(bandKey, REGULAR_BANDS.length)];
            addRenderableWidget(new EnchantmentRowWidget(x, y, panelWidth, 22,
                    Component.translatable(info.translationKey()), color));
            EnchantmentSelection selected = find(info.id());
            for (int level = 1; level <= info.maxLevel(); level++) {
                int selectedLevel = level;
                boolean isSelected = selected != null && selected.level == level;
                Component levelLabel = Component.literal(isSelected ? "[" + level + "]" : Integer.toString(level))
                        .withColor(isSelected ? 0x55FF55 : 0xFFFFFF);
                addRenderableWidget(Button.builder(levelLabel, button -> selectLevel(info, selectedLevel))
                        .bounds(x + labelWidth + (level - 1) * 38, y + 1, 34, 20).build());
            }
        }

        int navY = ROW_START_Y + rows * ROW_HEIGHT + 3;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            rebuildWidgetsPreservingInputs();
        }).bounds(x, navY, 30, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min(maxPage, page + 1);
            rebuildWidgetsPreservingInputs();
        }).bounds(x + 35, navY, 30, 20).build());
        next.active = page < maxPage;
        Component count = Component.translatable("gui.enhancement-order-hints.selected", working.enchantments.size());
        Button countButton = addRenderableWidget(Button.builder(count, button -> {})
                .bounds(x + 72, navY, 155, 20).build());
        countButton.active = false;
    }

    private void buildFooter(int x, int panelWidth) {
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> minecraft.setScreen(parent))
                .bounds(x, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.enhancement-order-hints.save"), button -> save())
                .bounds(x + panelWidth - 100, height - 28, 100, 20).build());
    }

    private void selectCategory(ItemCategory category) {
        preserveInputs();
        working.targetItem = category.id();
        int before = working.enchantments.size();
        working.enchantments.removeIf(selection -> !category.accepts(selection.id));
        int removed = before - working.enchantments.size();
        notice = removed == 0 ? Component.empty()
                : Component.translatable("gui.enhancement-order-hints.removed_inapplicable", removed);
        dropdownOpen = false;
        page = 0;
        rebuildWidgets();
    }

    private void selectLevel(EnchantmentInfo info, int level) {
        preserveInputs();
        EnchantmentSelection selected = find(info.id());
        if (selected != null && selected.level == level) {
            working.enchantments.remove(selected);
            notice = Component.empty();
            rebuildWidgets();
            return;
        }
        if (selected != null) {
            selected.level = level;
            notice = Component.empty();
            rebuildWidgets();
            return;
        }
        if (working.enchantments.size() >= 10) {
            notice = Component.translatable("gui.enhancement-order-hints.too_many");
            rebuildWidgets();
            return;
        }
        List<String> conflicts = EnchantmentCatalog.conflictsWith(info.id(),
                working.enchantments.stream().map(value -> value.id).toList());
        if (!conflicts.isEmpty()) {
            working.enchantments.removeIf(value -> conflicts.contains(value.id));
            notice = Component.translatable("gui.enhancement-order-hints.conflict_replaced", conflicts.size());
        } else {
            notice = Component.empty();
        }
        working.enchantments.add(new EnchantmentSelection(info.id(), level));
        rebuildWidgets();
    }

    private void save() {
        preserveInputs();
        if (working.name.isBlank() || working.enchantments.isEmpty()) {
            notice = Component.translatable("gui.enhancement-order-hints.invalid");
            return;
        }
        if (newPlan) ConfigManager.get().plans.add(working);
        else {
            for (int index = 0; index < ConfigManager.get().plans.size(); index++) {
                if (ConfigManager.get().plans.get(index).id.equals(working.id)) {
                    ConfigManager.get().plans.set(index, working);
                    break;
                }
            }
        }
        ConfigManager.get().selectedPlanId = working.id;
        ConfigManager.save();
        minecraft.setScreen(parent);
    }

    private List<EnchantmentInfo> displayedEnchantments() {
        return EnchantmentCatalog.forCategory(currentCategory());
    }

    private ItemCategory currentCategory() {
        return ItemCategory.fromStoredValue(working.targetItem);
    }

    private Component categoryLabel() {
        return Component.literal(dropdownOpen ? "▲ " : "▼ ")
                .append(Component.translatable(currentCategory().translationKey()));
    }

    private Component modeLabel() {
        String key = working.optimizationMode == OptimizationMode.EXPERIENCE
                ? "gui.enhancement-order-hints.mode_experience" : "gui.enhancement-order-hints.mode_penalty";
        return Component.translatable("gui.enhancement-order-hints.mode", Component.translatable(key));
    }

    private EnchantmentSelection find(String id) {
        return working.enchantments.stream().filter(value -> value.id.equals(id)).findFirst().orElse(null);
    }

    private void rebuildWidgetsPreservingInputs() {
        preserveInputs();
        rebuildWidgets();
    }

    private void preserveInputs() {
        if (nameBox != null) working.name = nameBox.getValue().trim();
    }

    private int panelWidth() {
        return Math.min(570, width - 24);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int visibleRows() {
        return Math.max(3, Math.min(MAX_ROWS, (height - ROW_START_Y - 60) / ROW_HEIGHT));
    }

    private int dropdownRows() {
        return Math.max(3, Math.min(6, (height - 82 - 60) / 22));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = panelX();
        graphics.text(font, Component.translatable("gui.enhancement-order-hints.name"), x, 38, 0xFFFFFFFF, false);
        graphics.text(font, Component.translatable("gui.enhancement-order-hints.item_category"), x, 64, 0xFFFFFFFF, false);
        if (!notice.getString().isEmpty() && !dropdownOpen) {
            int maxWidth = Math.max(20, panelWidth() - 235);
            String value = notice.getString();
            if (font.width(value) > maxWidth) value = font.plainSubstrByWidth(value, maxWidth - font.width("…")) + "…";
            graphics.text(font, value, x + 235, Math.min(height - 52, 307), 0xFFFFAA00, false);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
