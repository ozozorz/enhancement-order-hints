package dev.ozozorz.enhancementorderhints.enchant;

import java.util.Set;

public record EnchantmentInfo(String id, int maxLevel, int anvilWeight, Set<String> incompatible) {
    public String translationKey() {
        return "enchantment.minecraft." + id.substring(id.indexOf(':') + 1);
    }
}
