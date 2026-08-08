package dev.ozozorz.enhancementorderhints.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EnchantPlan {
    public String id = UUID.randomUUID().toString();
    public String name = "新附魔方案";
    public String targetItem = "sword";
    public OptimizationMode optimizationMode = OptimizationMode.EXPERIENCE;
    public List<EnchantmentSelection> enchantments = new ArrayList<>();

    public EnchantPlan copy() {
        EnchantPlan result = new EnchantPlan();
        result.id = id;
        result.name = name;
        result.targetItem = targetItem;
        result.optimizationMode = optimizationMode;
        result.enchantments = enchantments.stream().map(EnchantmentSelection::copy).toList();
        result.enchantments = new ArrayList<>(result.enchantments);
        return result;
    }
}
