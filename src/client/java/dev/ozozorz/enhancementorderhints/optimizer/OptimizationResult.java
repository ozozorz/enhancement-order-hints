package dev.ozozorz.enhancementorderhints.optimizer;

import java.util.List;

public record OptimizationResult(boolean possible, int totalLevels, int totalExperience,
                                 int priorWork, List<MergeStep> steps, String error) {
    public static OptimizationResult impossible(String error) {
        return new OptimizationResult(false, 0, 0, 0, List.of(), error);
    }

    public record MergeStep(Operand left, Operand right, Operand result, int levelCost, int priorWorkPenalty) {}

    public record Operand(String itemLabel, List<String> enchantments) {
        public Operand {
            enchantments = List.copyOf(enchantments);
        }

        public static Operand item(String label) {
            return new Operand(label, List.of());
        }

        public static Operand enchantment(String label) {
            return new Operand(null, List.of(label));
        }

        public static Operand combinedBook(List<String> enchantments) {
            return new Operand(null, enchantments);
        }

        public boolean isItem() {
            return itemLabel != null;
        }
    }
}
