package io.github.fishstiz.fidgetz.v0.gui.color;

public interface ColorModel {
    int toARGB();

    default float alpha() {
        return ((toARGB() >>> 24) & 0xFF) / 255f;
    }
}
