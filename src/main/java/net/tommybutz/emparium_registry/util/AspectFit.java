package net.tommybutz.emparium_registry.util;

public class AspectFit {

    public record Rect(int x1, int y1, int x2, int y2) {}

    public static Rect fit(int boxX1, int boxY1, int boxX2, int boxY2, float targetRatio) {
        int boxWidth = boxX2 - boxX1;
        int boxHeight = boxY2 - boxY1;
        float boxRatio = boxWidth / (float) boxHeight;

        int fitWidth, fitHeight;
        if (targetRatio > boxRatio) {
            fitWidth = boxWidth;
            fitHeight = Math.round(boxWidth / targetRatio);
        } else {
            fitHeight = boxHeight;
            fitWidth = Math.round(boxHeight * targetRatio);
        }

        int offsetX = (boxWidth - fitWidth) / 2;
        int offsetY = (boxHeight - fitHeight) / 2;

        return new Rect(boxX1 + offsetX, boxY1 + offsetY,
                boxX1 + offsetX + fitWidth, boxY1 + offsetY + fitHeight);
    }
}