package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

/**
 * TCWV main operator for Water_Vapour_cci.
 * Authors: R.Preusker (Python breadboard), O.Danne (Java conversion), 2018
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv", version = "0.8",
        authors = "R.Preusker, O.Danne",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Spectral Earth, Brockmann Consult",
        description = "TCWV main operator for Water_Vapour_cci.")
public class TcwvOp extends Operator {

    // todo

    @Override
    public void initialize() throws OperatorException {

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TcwvOp.class);
        }
    }
}
