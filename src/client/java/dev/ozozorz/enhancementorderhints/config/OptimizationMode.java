package dev.ozozorz.enhancementorderhints.config;

public enum OptimizationMode {
    EXPERIENCE,
    PRIOR_WORK_PENALTY;

    public OptimizationMode next() {
        return this == EXPERIENCE ? PRIOR_WORK_PENALTY : EXPERIENCE;
    }
}
