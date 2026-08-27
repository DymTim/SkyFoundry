package net.skyfoundry.core.progression.mission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class DailyMissionGenerator {

    private final Random random = new Random();

    public List<GeneratedMission> generate(
            int islandLevel,
            int requestedCount,
            DailyMissionRegistry registry) {
        List<DailyMissionDefinition> eligible = registry
                .getAll()
                .stream()
                .filter(
                        definition -> definition.isEligible(
                                islandLevel))
                .toList();

        if (eligible.isEmpty()) {
            return List.of();
        }

        List<DailyMissionDefinition> shuffled = new ArrayList<>(
                eligible);

        Collections.shuffle(
                shuffled,
                random);

        List<DailyMissionDefinition> selected = new ArrayList<>();

        Set<MissionCategory> usedCategories = new HashSet<>();

        /*
         * First pass:
         * favor distinct mission categories.
         */
        for (DailyMissionDefinition definition : shuffled) {

            if (selected.size() >= requestedCount) {

                break;
            }

            if (!usedCategories.add(
                    definition.category())) {

                continue;
            }

            selected.add(
                    definition);
        }

        /*
         * Second pass:
         * if there aren't enough categories,
         * fill remaining slots from any other
         * eligible template.
         */
        for (DailyMissionDefinition definition : shuffled) {

            if (selected.size() >= requestedCount) {

                break;
            }

            if (selected.contains(
                    definition)) {

                continue;
            }

            selected.add(
                    definition);
        }

        List<GeneratedMission> generated = new ArrayList<>();

        int slot = 0;

        for (DailyMissionDefinition definition : selected) {

            int target = calculateTarget(
                    definition,
                    islandLevel);

            generated.add(
                    new GeneratedMission(
                            slot++,
                            definition,
                            target));
        }

        return generated;
    }

    private int calculateTarget(
            DailyMissionDefinition definition,
            int islandLevel) {
        int minimum = definition.minimumAmount();

        int maximum = Math.max(
                minimum,
                definition.maximumAmount());

        int base;

        if (minimum == maximum) {
            base = minimum;

        } else {
            base = random.nextInt(
                    maximum - minimum + 1) + minimum;
        }

        int levelsAboveUnlock = Math.max(
                0,
                islandLevel
                        - definition
                                .minimumIslandLevel());

        long scaled = (long) base
                + ((long) levelsAboveUnlock
                        * definition
                                .amountPerLevel());

        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(
                        1,
                        scaled));
    }

    public record GeneratedMission(
            int slot,
            DailyMissionDefinition definition,
            int target) {
    }
}