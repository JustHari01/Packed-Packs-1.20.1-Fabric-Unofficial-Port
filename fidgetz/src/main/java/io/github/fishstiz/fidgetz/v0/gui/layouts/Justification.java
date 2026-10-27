package io.github.fishstiz.fidgetz.v0.gui.layouts;

public enum Justification {
    START,
    END,
    CENTER,
    STRETCH,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY;

    Results compute(int startPos, int spacing, int count, int freeSpace) {
        return switch (this) {
            case START -> new Results(startPos, spacing, 0);
            case END -> new Results(startPos + freeSpace, spacing, 0);
            case CENTER -> new Results(startPos + freeSpace / 2, spacing, freeSpace % 2);
            case STRETCH -> {
                int gaps = Math.max(1, count - 1);
                yield new Results(startPos, spacing, freeSpace % gaps);
            }
            case SPACE_BETWEEN -> {
                int gaps = Math.max(1, count - 1);
                yield new Results(startPos, count <= 1 ? spacing : spacing + freeSpace / (count - 1), freeSpace % gaps);
            }
            case SPACE_AROUND -> {
                int gap = freeSpace / count;
                yield new Results(startPos + gap / 2, spacing + gap, freeSpace % count);
            }
            case SPACE_EVENLY -> {
                int gap = freeSpace / (count + 1);
                yield new Results(startPos + gap, spacing + gap, freeSpace % (count + 1));
            }
        };
    }

    record Results(int pos, int spacing, int remainder) {
        int adjustSpacing(int index) {
            return spacing + (index < remainder ? 1 : 0);
        }
    }
}
