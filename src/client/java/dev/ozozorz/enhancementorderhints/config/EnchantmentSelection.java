package dev.ozozorz.enhancementorderhints.config;

public final class EnchantmentSelection {
    public String id;
    public int level;

    public EnchantmentSelection() {}

    public EnchantmentSelection(String id, int level) {
        this.id = id;
        this.level = level;
    }

    public EnchantmentSelection copy() {
        return new EnchantmentSelection(id, level);
    }
}
