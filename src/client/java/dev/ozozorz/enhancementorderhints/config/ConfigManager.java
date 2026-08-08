package dev.ozozorz.enhancementorderhints.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ozozorz.enhancementorderhints.EnhancementOrderHintsClient;
import dev.ozozorz.enhancementorderhints.enchant.ItemCategory;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("enhancement-order-hints.json");
    private static ModConfig config;

    private ConfigManager() {}

    public static synchronized ModConfig get() {
        if (config == null) load();
        return config;
    }

    public static synchronized void load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception exception) {
                EnhancementOrderHintsClient.LOGGER.error("无法读取配置 {}", PATH, exception);
            }
        }
        if (config == null) config = new ModConfig();
        sanitize(config);
    }

    public static synchronized void save() {
        sanitize(get());
        try {
            Files.createDirectories(PATH.getParent());
            Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            EnhancementOrderHintsClient.LOGGER.error("无法保存配置 {}", PATH, exception);
        }
    }

    public static Optional<EnchantPlan> selectedPlan() {
        ModConfig value = get();
        Optional<EnchantPlan> selected = value.plans.stream().filter(plan -> plan.id.equals(value.selectedPlanId)).findFirst();
        return selected.or(() -> value.plans.stream().findFirst());
    }

    private static void sanitize(ModConfig value) {
        if (value.plans == null) value.plans = new java.util.ArrayList<>();
        value.plans.removeIf(plan -> plan == null);
        value.plans.forEach(plan -> {
            if (plan.id == null || plan.id.isBlank()) plan.id = java.util.UUID.randomUUID().toString();
            if (plan.name == null || plan.name.isBlank()) plan.name = "未命名方案";
            plan.targetItem = ItemCategory.fromStoredValue(plan.targetItem).id();
            if (plan.optimizationMode == null) plan.optimizationMode = OptimizationMode.EXPERIENCE;
            if (plan.enchantments == null) plan.enchantments = new java.util.ArrayList<>();
            ItemCategory category = ItemCategory.fromStoredValue(plan.targetItem);
            plan.enchantments.removeIf(selection -> selection == null || selection.id == null || !category.accepts(selection.id));
        });
        if (value.selectedPlanId == null && !value.plans.isEmpty()) value.selectedPlanId = value.plans.getFirst().id;
    }
}
