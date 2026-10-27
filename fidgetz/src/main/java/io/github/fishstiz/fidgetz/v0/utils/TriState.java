package io.github.fishstiz.fidgetz.v0.utils;

// partially backported from 26.1
public enum TriState {
    DEFAULT,
    FALSE,
    TRUE;

    public static TriState from(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean toBoolean(boolean defaultValue) {
       return switch (this) {
           case TRUE ->  true;
           case FALSE -> false;
           default -> defaultValue;
       };
    }
}
