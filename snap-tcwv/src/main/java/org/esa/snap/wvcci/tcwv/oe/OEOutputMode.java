package org.esa.snap.wvcci.tcwv.oe;

/**
 * Enumeration for output mode of OE optimization.
 *
 * @author olafd
 */
public enum OEOutputMode {
    BASIC("basic"),
    FULL("full"),
    EXTENDED("extended");

    private final String name;

    OEOutputMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
