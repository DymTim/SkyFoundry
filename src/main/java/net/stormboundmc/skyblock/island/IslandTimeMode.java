package net.stormboundmc.skyblock.island;

public enum IslandTimeMode {
    DEFAULT(-1L),
    SUNRISE(0L),
    NOON(6000L),
    SUNSET(12000L),
    MIDNIGHT(18000L);

    private final long ticks;

    IslandTimeMode(long ticks) {
        this.ticks = ticks;
    }

    public long getTicks() {
        return ticks;
    }
}
