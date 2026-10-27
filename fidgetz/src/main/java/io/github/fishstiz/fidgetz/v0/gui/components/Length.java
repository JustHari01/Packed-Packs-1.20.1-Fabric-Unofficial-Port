package io.github.fishstiz.fidgetz.v0.gui.components;

public sealed interface Length {
    int resolve(int basis);

    static Length zero() {
        return Fixed.ZERO;
    }

    static Length fill() {
        return Relative.FILL;
    }

    static Length fixed(int value) {
        return value == 0 ? Fixed.ZERO : new Fixed(value);
    }

    static Length relative(float ratio) {
        return ratio == 1.0f ? Relative.FILL : new Relative(ratio);
    }

    record Fixed(int value) implements Length {
        private static final Fixed ZERO = new Fixed(0);

        @Override
        public int resolve(int basis) {
            return value;
        }
    }

    record Relative(float ratio) implements Length {
        private static final Relative FILL = new Relative(1.0f);

        @Override
        public int resolve(int basis) {
            return Math.round(ratio * basis);
        }
    }
}
