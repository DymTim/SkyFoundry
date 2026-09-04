package net.stormboundmc.skyblock.island;

public final class IslandSettings {

    private final long islandId;
    private boolean memberBuilding;
    private boolean memberInteractions;
    private boolean visitingEnabled;
    private IslandWeatherMode weatherMode;
    private IslandTimeMode timeMode;
    private boolean borderEnabled;

    public IslandSettings(
            long islandId,
            boolean memberBuilding,
            boolean memberInteractions,
            boolean visitingEnabled,
            IslandWeatherMode weatherMode,
            IslandTimeMode timeMode,
            boolean borderEnabled
    ) {
        this.islandId = islandId;
        this.memberBuilding = memberBuilding;
        this.memberInteractions = memberInteractions;
        this.visitingEnabled = visitingEnabled;
        this.weatherMode = weatherMode;
        this.timeMode = timeMode;
        this.borderEnabled = borderEnabled;
    }

    public static IslandSettings defaults(long islandId) {
        return new IslandSettings(
                islandId,
                true,
                true,
                true,
                IslandWeatherMode.DEFAULT,
                IslandTimeMode.DEFAULT,
                false
        );
    }

    public long getIslandId() {
        return islandId;
    }

    public boolean isMemberBuilding() {
        return memberBuilding;
    }

    public void setMemberBuilding(boolean memberBuilding) {
        this.memberBuilding = memberBuilding;
    }

    public boolean isMemberInteractions() {
        return memberInteractions;
    }

    public void setMemberInteractions(boolean memberInteractions) {
        this.memberInteractions = memberInteractions;
    }

    public boolean isVisitingEnabled() {
        return visitingEnabled;
    }

    public void setVisitingEnabled(boolean visitingEnabled) {
        this.visitingEnabled = visitingEnabled;
    }

    public IslandWeatherMode getWeatherMode() {
        return weatherMode;
    }

    public void setWeatherMode(IslandWeatherMode weatherMode) {
        this.weatherMode = weatherMode;
    }

    public IslandTimeMode getTimeMode() {
        return timeMode;
    }

    public void setTimeMode(IslandTimeMode timeMode) {
        this.timeMode = timeMode;
    }

    public boolean isBorderEnabled() {
        return borderEnabled;
    }

    public void setBorderEnabled(boolean borderEnabled) {
        this.borderEnabled = borderEnabled;
    }
}
