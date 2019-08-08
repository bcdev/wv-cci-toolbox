package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.binning.VariableContext;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 24.06.2019
 * Time: 11:38
 *
 * @author olafd
 */
public class MyVariableContext implements VariableContext {

    private String[] varNames;

    MyVariableContext(String... varNames) {
        this.varNames = varNames;
    }

    @Override
    public int getVariableCount() {
        return varNames.length;
    }

    @Override
    public String getVariableName(int i) {
        return varNames[i];
    }

    @Override
    public int getVariableIndex(String name) {
        for (int i = 0; i < varNames.length; i++) {
            if (name.equals(varNames[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getVariableExpression(int i) {
        return null;
    }

    @Override
    public String getVariableValidExpression(int index) {
        return null;
    }

    @Override
    public String getValidMaskExpression() {
        return null;
    }
}
