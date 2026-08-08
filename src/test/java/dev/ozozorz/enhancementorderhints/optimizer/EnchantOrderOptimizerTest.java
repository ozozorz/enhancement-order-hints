package dev.ozozorz.enhancementorderhints.optimizer;

import dev.ozozorz.enhancementorderhints.config.EnchantPlan;
import dev.ozozorz.enhancementorderhints.config.EnchantmentSelection;
import dev.ozozorz.enhancementorderhints.config.OptimizationMode;
import dev.ozozorz.enhancementorderhints.enchant.EnchantmentCatalog;
import dev.ozozorz.enhancementorderhints.enchant.EnchantmentInfo;
import dev.ozozorz.enhancementorderhints.enchant.ItemCategory;
import dev.ozozorz.enhancementorderhints.render.PanelLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnchantOrderOptimizerTest {
    @Test
    void returnsACompletePlan() {
        EnchantPlan plan = new EnchantPlan();
        plan.name = "剑";
        plan.enchantments = new ArrayList<>(List.of(
                new EnchantmentSelection("minecraft:sharpness", 5),
                new EnchantmentSelection("minecraft:looting", 3),
                new EnchantmentSelection("minecraft:unbreaking", 3),
                new EnchantmentSelection("minecraft:mending", 1)
        ));

        OptimizationResult result = EnchantOrderOptimizer.optimize(plan);

        assertTrue(result.possible());
        assertEquals(4, result.steps().size());
        assertTrue(result.steps().stream().allMatch(step -> step.levelCost() <= 39));
        OptimizationResult.Operand combinedBook = result.steps().stream()
                .map(OptimizationResult.MergeStep::result)
                .filter(operand -> !operand.isItem() && operand.enchantments().size() > 1)
                .findFirst().orElseThrow();
        assertTrue(result.steps().stream().anyMatch(step -> step.right().equals(combinedBook)));
        assertTrue(combinedBook.enchantments().stream().allMatch(value -> value.startsWith("minecraft:")));
    }

    @Test
    void penaltyModeMinimizesFinalPenalty() {
        EnchantPlan plan = new EnchantPlan();
        plan.optimizationMode = OptimizationMode.PRIOR_WORK_PENALTY;
        plan.enchantments = new ArrayList<>(List.of(
                new EnchantmentSelection("sharpness", 5),
                new EnchantmentSelection("looting", 3),
                new EnchantmentSelection("unbreaking", 3),
                new EnchantmentSelection("mending", 1)
        ));
        OptimizationResult result = EnchantOrderOptimizer.optimize(plan);
        assertTrue(result.possible());
        assertTrue(result.priorWork() <= 7);
    }

    @Test
    void detectsConflictsSymmetrically() {
        assertTrue(EnchantmentCatalog.conflicts("minecraft:fortune", "minecraft:silk_touch"));
        assertTrue(EnchantmentCatalog.conflicts("minecraft:silk_touch", "minecraft:fortune"));
        assertFalse(EnchantmentCatalog.conflicts("minecraft:unbreaking", "minecraft:mending"));
    }

    @Test
    void filtersAndGroupsEnchantmentsForSword() {
        List<EnchantmentInfo> sword = EnchantmentCatalog.forCategory(ItemCategory.SWORD);
        List<String> ids = sword.stream().map(EnchantmentInfo::id).toList();
        assertFalse(ids.contains("minecraft:protection"));
        assertTrue(ids.contains("minecraft:sharpness"));
        assertEquals(List.of("minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods"),
                ids.subList(0, 3));
        assertEquals(EnchantmentCatalog.bandKey(sword.get(0), sword),
                EnchantmentCatalog.bandKey(sword.get(2), sword));
    }

    @Test
    void migratesMaterialSpecificItemIdsToCategories() {
        assertEquals(ItemCategory.SWORD, ItemCategory.fromStoredValue("minecraft:diamond_sword"));
        assertEquals(ItemCategory.PICKAXE, ItemCategory.fromStoredValue("minecraft:netherite_pickaxe"));
        assertEquals(ItemCategory.HELMET, ItemCategory.fromStoredValue("minecraft:turtle_helmet"));
    }

    @Test
    void hintPanelNeverOverlapsTheAnvil() {
        PanelLayout.Bounds right = PanelLayout.find(640, 232, 176);
        assertNotNull(right);
        assertTrue(right.x() >= 232 + 176 + 8);
        assertTrue(right.x() + right.width() <= 636);

        PanelLayout.Bounds left = PanelLayout.find(420, 220, 176);
        assertNotNull(left);
        assertTrue(left.x() + left.width() <= 220 - 8);

        assertNull(PanelLayout.find(300, 62, 176));
    }

    @Test
    void hintPanelExpandsVerticallyAndStaysOnScreen() {
        PanelLayout.VerticalBounds expanded = PanelLayout.findVertical(540, 187, 166, 300);
        assertNotNull(expanded);
        assertEquals(300, expanded.height());
        assertTrue(expanded.y() < 187);
        assertTrue(expanded.y() + expanded.height() > 187 + 166);

        PanelLayout.VerticalBounds clamped = PanelLayout.findVertical(540, 187, 166, 1_000);
        assertNotNull(clamped);
        assertEquals(4, clamped.y());
        assertEquals(532, clamped.height());
    }
}
