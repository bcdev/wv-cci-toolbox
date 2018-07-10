package org.esa.snap.wvcci.tcwv;

/**
 * Enumeration for method used for Optimal Estimation
 *
 * @author olafd
 */
public enum InversionMethod {
     NEWTON("NEWTON"),
     NEWTON_SE("NEWTON_SE"),
     OE("OE");

    private final String name;

    InversionMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
