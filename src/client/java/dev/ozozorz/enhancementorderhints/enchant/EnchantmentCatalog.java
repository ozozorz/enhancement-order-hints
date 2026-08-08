package dev.ozozorz.enhancementorderhints.enchant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnchantmentCatalog {
    private static final Map<String, EnchantmentInfo> VALUES = new LinkedHashMap<>();

    static {
        add("protection", 4, 1, "blast_protection", "fire_protection", "projectile_protection");
        add("fire_protection", 4, 1, "protection", "blast_protection", "projectile_protection");
        add("feather_falling", 4, 1);
        add("blast_protection", 4, 2, "protection", "fire_protection", "projectile_protection");
        add("projectile_protection", 4, 1, "protection", "fire_protection", "blast_protection");
        add("respiration", 3, 2);
        add("aqua_affinity", 1, 2);
        add("thorns", 3, 4);
        add("depth_strider", 3, 2, "frost_walker");
        add("frost_walker", 2, 2, "depth_strider");
        add("binding_curse", 1, 4);
        add("soul_speed", 3, 4);
        add("swift_sneak", 3, 4);
        add("sharpness", 5, 1, "smite", "bane_of_arthropods");
        add("smite", 5, 1, "sharpness", "bane_of_arthropods", "density", "breach");
        add("bane_of_arthropods", 5, 1, "sharpness", "smite", "density", "breach");
        add("knockback", 2, 1);
        add("fire_aspect", 2, 2);
        add("looting", 3, 2);
        add("sweeping_edge", 3, 2);
        add("efficiency", 5, 1);
        add("silk_touch", 1, 4, "fortune");
        add("unbreaking", 3, 1);
        add("fortune", 3, 2, "silk_touch");
        add("power", 5, 1);
        add("punch", 2, 2);
        add("flame", 1, 2);
        add("infinity", 1, 4, "mending");
        add("luck_of_the_sea", 3, 2);
        add("lure", 3, 2);
        add("loyalty", 3, 1, "riptide");
        add("impaling", 5, 2);
        add("riptide", 3, 2, "loyalty", "channeling");
        add("channeling", 1, 4, "riptide");
        add("multishot", 1, 2, "piercing");
        add("quick_charge", 3, 1);
        add("piercing", 4, 1, "multishot");
        add("mending", 1, 2, "infinity");
        add("vanishing_curse", 1, 4);
        add("density", 5, 1, "breach", "smite", "bane_of_arthropods");
        add("breach", 4, 2, "density", "smite", "bane_of_arthropods");
        add("wind_burst", 3, 2);
        add("lunge", 3, 1);
    }

    private EnchantmentCatalog() {}

    public static List<EnchantmentInfo> all() {
        return List.copyOf(VALUES.values());
    }

    public static List<EnchantmentInfo> forCategory(ItemCategory category) {
        List<EnchantmentInfo> applicable = VALUES.values().stream()
                .filter(info -> category.accepts(info.id())).toList();
        List<EnchantmentInfo> grouped = new ArrayList<>();
        Set<String> added = new java.util.HashSet<>();
        for (EnchantmentInfo info : applicable) {
            if (added.contains(info.id())) continue;
            List<EnchantmentInfo> component = conflictComponent(info, applicable);
            for (EnchantmentInfo member : applicable) {
                if (component.contains(member) && added.add(member.id())) grouped.add(member);
            }
        }
        return List.copyOf(grouped);
    }

    public static int bandKey(EnchantmentInfo info, List<EnchantmentInfo> displayed) {
        List<EnchantmentInfo> component = conflictComponent(info, displayed);
        int first = displayed.indexOf(info);
        for (EnchantmentInfo member : component) first = Math.min(first, displayed.indexOf(member));
        return first;
    }

    public static boolean hasConflictGroup(EnchantmentInfo info, List<EnchantmentInfo> displayed) {
        return conflictComponent(info, displayed).size() > 1;
    }

    public static EnchantmentInfo get(String id) {
        return VALUES.get(normalize(id));
    }

    public static boolean conflicts(String first, String second) {
        String a = normalize(first);
        String b = normalize(second);
        EnchantmentInfo left = VALUES.get(a);
        EnchantmentInfo right = VALUES.get(b);
        return left != null && left.incompatible().contains(b)
                || right != null && right.incompatible().contains(a);
    }

    public static List<String> conflictsWith(String candidate, Iterable<String> selected) {
        List<String> result = new ArrayList<>();
        for (String id : selected) if (conflicts(candidate, id)) result.add(normalize(id));
        return result;
    }

    private static void add(String id, int maxLevel, int weight, String... incompatible) {
        String fullId = normalize(id);
        Set<String> conflicts = java.util.Arrays.stream(incompatible).map(EnchantmentCatalog::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        VALUES.put(fullId, new EnchantmentInfo(fullId, maxLevel, weight, conflicts));
    }

    private static List<EnchantmentInfo> conflictComponent(EnchantmentInfo start, List<EnchantmentInfo> values) {
        List<EnchantmentInfo> result = new ArrayList<>();
        java.util.ArrayDeque<EnchantmentInfo> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            EnchantmentInfo current = queue.removeFirst();
            if (result.contains(current)) continue;
            result.add(current);
            for (EnchantmentInfo candidate : values) {
                if (!result.contains(candidate) && conflicts(current.id(), candidate.id())) queue.add(candidate);
            }
        }
        return result;
    }

    private static String normalize(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
