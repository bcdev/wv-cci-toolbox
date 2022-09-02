package org.esa.snap.wvcci.tcwv.dataio.erainterim;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.junit.Test;

import java.util.Iterator;

import static junit.framework.TestCase.assertEquals;

/**
 *
 * @author olafd
 */
public class ScripGeocodingNc4WriterLoadedAsServiceTest {
    @Test
    public void testWriterPlugInIsLoaded() {
        int writerCount = 0;

        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator writerPlugIns = plugInManager.getWriterPlugIns("SCRIP_NC4");

        while (writerPlugIns.hasNext()) {
            writerCount++;
            ProductWriterPlugIn plugIn = (ProductWriterPlugIn) writerPlugIns.next();
            System.out.println("writerPlugIn.Class = " + plugIn.getClass());
            System.out.println("writerPlugIn.Descr = " + plugIn.getDescription(null));
        }

        assertEquals(1, writerCount);

    }

}
