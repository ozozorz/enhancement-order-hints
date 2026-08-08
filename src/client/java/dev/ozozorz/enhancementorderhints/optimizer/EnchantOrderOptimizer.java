package dev.ozozorz.enhancementorderhints.optimizer;

import dev.ozozorz.enhancementorderhints.config.EnchantPlan;
import dev.ozozorz.enhancementorderhints.config.EnchantmentSelection;
import dev.ozozorz.enhancementorderhints.config.OptimizationMode;
import dev.ozozorz.enhancementorderhints.enchant.EnchantmentCatalog;
import dev.ozozorz.enhancementorderhints.enchant.EnchantmentInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantOrderOptimizer {
    private static final int MAX_MERGE_LEVELS = 39;
    private static final int MAX_ENCHANTMENTS = 10;

    private EnchantOrderOptimizer() {}

    public static OptimizationResult optimize(EnchantPlan plan) {
        if (plan == null || plan.enchantments == null || plan.enchantments.isEmpty()) {
            return OptimizationResult.impossible("empty");
        }
        if (plan.enchantments.size() > MAX_ENCHANTMENTS) {
            return OptimizationResult.impossible("too_many");
        }

        List<Leaf> leaves = new ArrayList<>();
        leaves.add(new Leaf(plan.name, 0, true));
        for (EnchantmentSelection selection : plan.enchantments) {
            EnchantmentInfo info = EnchantmentCatalog.get(selection.id);
            if (info == null) return OptimizationResult.impossible("unknown:" + selection.id);
            int level = Math.max(1, Math.min(selection.level, info.maxLevel()));
            leaves.add(new Leaf(selection.id + " " + roman(level), level * info.anvilWeight(), false));
        }

        Solver solver = new Solver(leaves);
        int fullMask = (1 << leaves.size()) - 1;
        Map<Integer, Candidate> candidates = solver.solve(fullMask);
        if (candidates.isEmpty()) return OptimizationResult.impossible("too_expensive");

        Comparator<Candidate> comparator;
        if (plan.optimizationMode == OptimizationMode.PRIOR_WORK_PENALTY) {
            comparator = Comparator.comparingInt((Candidate value) -> value.work)
                    .thenComparingInt(value -> value.totalXp)
                    .thenComparingInt(value -> value.totalLevels);
        } else {
            comparator = Comparator.comparingInt((Candidate value) -> value.totalXp)
                    .thenComparingInt(value -> value.totalLevels)
                    .thenComparingInt(value -> value.work);
        }
        Candidate best = candidates.values().stream().min(comparator).orElseThrow();
        List<OptimizationResult.MergeStep> steps = new ArrayList<>();
        describe(best, steps);
        return new OptimizationResult(true, best.totalLevels, best.totalXp, penalty(best.work), List.copyOf(steps), null);
    }

    private static OptimizationResult.Operand describe(Candidate candidate, List<OptimizationResult.MergeStep> steps) {
        if (candidate.leaf != null) {
            return candidate.leaf.target
                    ? OptimizationResult.Operand.item(candidate.leaf.label)
                    : OptimizationResult.Operand.enchantment(candidate.leaf.label);
        }
        OptimizationResult.Operand left = describe(candidate.left, steps);
        OptimizationResult.Operand right = describe(candidate.right, steps);
        OptimizationResult.Operand result = candidate.containsTarget
                ? OptimizationResult.Operand.item(candidate.targetLabel())
                : OptimizationResult.Operand.combinedBook(candidate.enchantmentLabels());
        steps.add(new OptimizationResult.MergeStep(left, right, result, candidate.mergeLevels, penalty(candidate.work)));
        return result;
    }

    private static int xpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (9 * level * level - 325 * level + 4440) / 2;
    }

    private static int penalty(int work) {
        return work <= 0 ? 0 : (1 << Math.min(work, 30)) - 1;
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    private record Leaf(String label, int value, boolean target) {}

    private static final class Candidate {
        private final Leaf leaf;
        private final Candidate left;
        private final Candidate right;
        private final int value;
        private final int work;
        private final int totalLevels;
        private final int totalXp;
        private final int mergeLevels;
        private final boolean containsTarget;

        private Candidate(Leaf leaf) {
            this.leaf = leaf;
            left = null;
            right = null;
            value = leaf.value;
            work = 0;
            totalLevels = 0;
            totalXp = 0;
            mergeLevels = 0;
            containsTarget = leaf.target;
        }

        private Candidate(Candidate left, Candidate right, int mergeLevels) {
            leaf = null;
            this.left = left;
            this.right = right;
            value = left.value + right.value;
            work = Math.max(left.work, right.work) + 1;
            totalLevels = left.totalLevels + right.totalLevels + mergeLevels;
            totalXp = left.totalXp + right.totalXp + xpForLevel(mergeLevels);
            this.mergeLevels = mergeLevels;
            containsTarget = left.containsTarget || right.containsTarget;
        }

        private String targetLabel() {
            if (leaf != null && leaf.target) return leaf.label;
            if (left != null && left.containsTarget) return left.targetLabel();
            return right.targetLabel();
        }

        private List<String> enchantmentLabels() {
            List<String> result = new ArrayList<>();
            collectEnchantmentLabels(result);
            return result;
        }

        private void collectEnchantmentLabels(List<String> result) {
            if (leaf != null) {
                if (!leaf.target) result.add(leaf.label);
                return;
            }
            left.collectEnchantmentLabels(result);
            right.collectEnchantmentLabels(result);
        }
    }

    private static final class Solver {
        private final List<Leaf> leaves;
        private final Map<Integer, Map<Integer, Candidate>> memo = new HashMap<>();

        private Solver(List<Leaf> leaves) {
            this.leaves = leaves;
        }

        private Map<Integer, Candidate> solve(int mask) {
            Map<Integer, Candidate> cached = memo.get(mask);
            if (cached != null) return cached;
            if (Integer.bitCount(mask) == 1) {
                int index = Integer.numberOfTrailingZeros(mask);
                Map<Integer, Candidate> result = Map.of(0, new Candidate(leaves.get(index)));
                memo.put(mask, result);
                return result;
            }

            Map<Integer, Candidate> result = new LinkedHashMap<>();
            for (int leftMask = (mask - 1) & mask; leftMask != 0; leftMask = (leftMask - 1) & mask) {
                int rightMask = mask ^ leftMask;
                if (rightMask == 0 || leftMask > rightMask) continue;
                for (Candidate first : solve(leftMask).values()) {
                    for (Candidate second : solve(rightMask).values()) {
                        tryMerge(first, second, result);
                        tryMerge(second, first, result);
                    }
                }
            }
            memo.put(mask, result);
            return result;
        }

        private void tryMerge(Candidate left, Candidate right, Map<Integer, Candidate> result) {
            if (right.containsTarget) return;
            int mergeLevels = right.value + penalty(left.work) + penalty(right.work);
            if (mergeLevels > MAX_MERGE_LEVELS) return;
            Candidate candidate = new Candidate(left, right, mergeLevels);
            Candidate previous = result.get(candidate.work);
            if (previous == null || candidate.totalXp < previous.totalXp
                    || candidate.totalXp == previous.totalXp && candidate.totalLevels < previous.totalLevels) {
                result.put(candidate.work, candidate);
            }
        }
    }
}
