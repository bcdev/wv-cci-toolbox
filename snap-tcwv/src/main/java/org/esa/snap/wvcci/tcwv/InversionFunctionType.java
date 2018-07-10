package org.esa.snap.wvcci.tcwv;

/**
 * Enumeration for function type used in Optimal Estimation
 *
 * @author olafd
 */
public enum InversionFunctionType {
    LINEAR_R2_R3("LINEAR_R2_R3"),
    NONLINEAR_R2_R3("NONLINEAR_R2_R3"),
    LINEAR_R3_R2("LINEAR_R3_R2");

    private final String name;

    InversionFunctionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
