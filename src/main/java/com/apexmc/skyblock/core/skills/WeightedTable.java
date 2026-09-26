package com.apexmc.skyblock.core.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Deterministic weighted picker used by fishing loot, generator outputs and
 * rare drops. Injecting the {@link RandomGenerator} keeps the maths unit
 * testable and free of hidden state.
 */
public final class WeightedTable<T> {

    /** One weighted entry. */
    public record Entry<T>(T value, double weight) {
    }

    private final List<Entry<T>> entries;
    private final double totalWeight;

    public WeightedTable(List<Entry<T>> entries) {
        List<Entry<T>> valid = new ArrayList<>();
        double total = 0.0d;
        for (Entry<T> entry : entries) {
            if (entry.weight() > 0) {
                valid.add(entry);
                total += entry.weight();
            }
        }
        this.entries = List.copyOf(valid);
        this.totalWeight = total;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry<T>> entries() {
        return entries;
    }

    public double totalWeight() {
        return totalWeight;
    }

    public double chanceOf(T value) {
        if (totalWeight <= 0) {
            return 0.0d;
        }
        double weight = entries.stream().filter(entry -> entry.value().equals(value))
                .mapToDouble(Entry::weight).sum();
        return weight / totalWeight;
    }

    public Optional<T> pick(RandomGenerator random) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        double roll = random.nextDouble() * totalWeight;
        double cursor = 0.0d;
        for (Entry<T> entry : entries) {
            cursor += entry.weight();
            if (roll < cursor) {
                return Optional.of(entry.value());
            }
        }
        return Optional.of(entries.get(entries.size() - 1).value());
    }

    /** Picks using an explicit roll in [0,1) - used by tests for determinism. */
    public Optional<T> pickAt(double normalisedRoll) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        double roll = Math.max(0.0d, Math.min(0.999999d, normalisedRoll)) * totalWeight;
        double cursor = 0.0d;
        for (Entry<T> entry : entries) {
            cursor += entry.weight();
            if (roll < cursor) {
                return Optional.of(entry.value());
            }
        }
        return Optional.of(entries.get(entries.size() - 1).value());
    }
}
