package dev.ozozorz.enhancementorderhints.enchant;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum ItemCategory {
    SWORD("sword", "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge", "unbreaking", "mending", "vanishing_curse"),
    AXE("axe", "sharpness", "smite", "bane_of_arthropods", "efficiency", "silk_touch", "fortune", "unbreaking", "mending", "vanishing_curse"),
    MACE("mace", "smite", "bane_of_arthropods", "density", "breach", "wind_burst", "fire_aspect", "unbreaking", "mending", "vanishing_curse"),
    SPEAR("spear", "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "lunge", "unbreaking", "mending", "vanishing_curse"),
    TRIDENT("trident", "loyalty", "impaling", "riptide", "channeling", "unbreaking", "mending", "vanishing_curse"),
    BOW("bow", "power", "punch", "flame", "infinity", "unbreaking", "mending", "vanishing_curse"),
    CROSSBOW("crossbow", "multishot", "piercing", "quick_charge", "unbreaking", "mending", "vanishing_curse"),
    PICKAXE("pickaxe", "efficiency", "silk_touch", "fortune", "unbreaking", "mending", "vanishing_curse"),
    SHOVEL("shovel", "efficiency", "silk_touch", "fortune", "unbreaking", "mending", "vanishing_curse"),
    HOE("hoe", "efficiency", "silk_touch", "fortune", "unbreaking", "mending", "vanishing_curse"),
    HELMET("helmet", "protection", "fire_protection", "blast_protection", "projectile_protection", "respiration", "aqua_affinity", "thorns", "unbreaking", "mending", "binding_curse", "vanishing_curse"),
    CHESTPLATE("chestplate", "protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "unbreaking", "mending", "binding_curse", "vanishing_curse"),
    LEGGINGS("leggings", "protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "swift_sneak", "unbreaking", "mending", "binding_curse", "vanishing_curse"),
    BOOTS("boots", "protection", "fire_protection", "blast_protection", "projectile_protection", "feather_falling", "thorns", "depth_strider", "frost_walker", "soul_speed", "unbreaking", "mending", "binding_curse", "vanishing_curse"),
    ELYTRA("elytra", "unbreaking", "mending", "binding_curse", "vanishing_curse"),
    FISHING_ROD("fishing_rod", "luck_of_the_sea", "lure", "unbreaking", "mending", "vanishing_curse"),
    SHEARS("shears", "efficiency", "unbreaking", "mending", "vanishing_curse"),
    SHIELD("shield", "unbreaking", "mending", "vanishing_curse"),
    BRUSH("brush", "unbreaking", "mending", "vanishing_curse"),
    FLINT_AND_STEEL("flint_and_steel", "unbreaking", "mending", "vanishing_curse"),
    RIDING_ROD("riding_rod", "unbreaking", "mending", "vanishing_curse");

    private final String id;
    private final Set<String> enchantments;

    ItemCategory(String id, String... enchantments) {
        this.id = id;
        this.enchantments = Arrays.stream(enchantments)
                .map(value -> "minecraft:" + value)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "item_category.enhancement-order-hints." + id;
    }

    public boolean accepts(String enchantmentId) {
        String normalized = enchantmentId.contains(":") ? enchantmentId : "minecraft:" + enchantmentId;
        return enchantments.contains(normalized);
    }

    public static ItemCategory fromStoredValue(String value) {
        return detectItemId(value).orElse(SWORD);
    }

    public static Optional<ItemCategory> detectItemId(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT);
        for (ItemCategory category : values()) if (category.id.equals(normalized)) return Optional.of(category);
        String path = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        if (path.endsWith("_pickaxe")) return Optional.of(PICKAXE);
        if (path.endsWith("_sword")) return Optional.of(SWORD);
        if (path.endsWith("_axe")) return Optional.of(AXE);
        if (path.endsWith("_shovel")) return Optional.of(SHOVEL);
        if (path.endsWith("_hoe")) return Optional.of(HOE);
        if (path.endsWith("_helmet") || path.equals("turtle_helmet")) return Optional.of(HELMET);
        if (path.endsWith("_chestplate")) return Optional.of(CHESTPLATE);
        if (path.endsWith("_leggings")) return Optional.of(LEGGINGS);
        if (path.endsWith("_boots")) return Optional.of(BOOTS);
        ItemCategory detected = switch (path) {
            case "mace" -> MACE;
            case "spear" -> SPEAR;
            case "trident" -> TRIDENT;
            case "bow" -> BOW;
            case "crossbow" -> CROSSBOW;
            case "elytra" -> ELYTRA;
            case "fishing_rod" -> FISHING_ROD;
            case "shears" -> SHEARS;
            case "shield" -> SHIELD;
            case "brush" -> BRUSH;
            case "flint_and_steel" -> FLINT_AND_STEEL;
            case "carrot_on_a_stick", "warped_fungus_on_a_stick" -> RIDING_ROD;
            default -> null;
        };
        return Optional.ofNullable(detected);
    }

    public static ItemCategory fromItemId(String itemId) {
        return fromStoredValue(itemId);
    }
}
