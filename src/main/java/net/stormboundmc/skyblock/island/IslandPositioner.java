package net.stormboundmc.skyblock.island;

public final class IslandPositioner {

    private final int spacing;

    public IslandPositioner(int spacing) {
        if (spacing <= 0) {
            throw new IllegalArgumentException(
                    "Island spacing must be greater than zero.");
        }

        this.spacing = spacing;
    }

    public IslandPosition getPosition(long index) {
        if (index < 0) {
            throw new IllegalArgumentException(
                    "Island index cannot be negative.");
        }

        if (index == 0) {
            return new IslandPosition(0, 0);
        }

        long layer = (long) Math.ceil(
                (Math.sqrt(index + 1) - 1) / 2);

        long legLength = layer * 2;
        long maxIndex = (2 * layer + 1) * (2 * layer + 1) - 1;
        long offset = maxIndex - index;

        long gridX;
        long gridZ;

        if (offset < legLength) {
            gridX = layer - offset;
            gridZ = layer;
        } else if (offset < legLength * 2) {
            offset -= legLength;

            gridX = -layer;
            gridZ = layer - offset;
        } else if (offset < legLength * 3) {
            offset -= legLength * 2;

            gridX = -layer + offset;
            gridZ = -layer;
        } else {
            offset -= legLength * 3;

            gridX = layer;
            gridZ = -layer + offset;
        }

        return new IslandPosition(
                Math.toIntExact(gridX * spacing),
                Math.toIntExact(gridZ * spacing));
    }

    public record IslandPosition(int x, int z) {
    }
}