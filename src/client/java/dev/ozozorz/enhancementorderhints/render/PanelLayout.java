package dev.ozozorz.enhancementorderhints.render;

public final class PanelLayout {
    private static final int MARGIN = 4;
    private static final int GAP = 8;
    private static final int MIN_WIDTH = 128;
    private static final int PREFERRED_WIDTH = 160;

    private PanelLayout() {}

    public static Bounds find(int screenWidth, int anvilLeft, int anvilWidth) {
        int anvilRight = anvilLeft + anvilWidth;
        int rightX = anvilRight + GAP;
        int rightAvailable = screenWidth - MARGIN - rightX;
        if (rightAvailable >= MIN_WIDTH) {
            return new Bounds(rightX, Math.min(PREFERRED_WIDTH, rightAvailable));
        }

        int leftAvailable = anvilLeft - GAP - MARGIN;
        if (leftAvailable >= MIN_WIDTH) {
            int width = Math.min(PREFERRED_WIDTH, leftAvailable);
            return new Bounds(anvilLeft - GAP - width, width);
        }
        return null;
    }

    public static VerticalBounds findVertical(int screenHeight, int anvilTop, int anvilHeight, int desiredHeight) {
        int availableHeight = screenHeight - MARGIN * 2;
        if (availableHeight < 70) return null;

        int height = Math.min(availableHeight, Math.max(anvilHeight, desiredHeight));
        int anvilCenter = anvilTop + anvilHeight / 2;
        int y = anvilCenter - height / 2;
        y = Math.max(MARGIN, Math.min(y, screenHeight - MARGIN - height));
        return new VerticalBounds(y, height);
    }

    public record Bounds(int x, int width) {}

    public record VerticalBounds(int y, int height) {}
}
