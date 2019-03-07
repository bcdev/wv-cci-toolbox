package org.esa.snap.wvcci.tcwv.dataio.erainterim;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.junit.Test;

import java.util.Iterator;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

public class EraInterimProductReaderLoadedAsServiceTest {

    @Test
    public void testReaderPlugInIsLoaded() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("ERA-INTERIM");

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(EraInterimProductReaderPlugin.class, plugIn.getClass());
        } else {
            fail(String.format("Where is %s?", EraInterimProductReaderPlugin.class.getSimpleName()));
        }
    }

}