package org.esa.snap.wvcci.tcwv.dataio.mod35;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ModisMod35L2UtilsTest {

    @Test
    public void testGetProductDimensions() {
        // StructMetadata.0 string in MOD35 L2 HDF4 input file:
        String structMetadata0String = "GROUP=SwathStructure\n" +
                "\tGROUP=SWATH_1\n" +
                "\t\tSwathName=\"mod35\"\n" +
                "\t\tGROUP=Dimension\n" +
                "\t\t\tOBJECT=Dimension_1\n" +
                "\t\t\t\tDimensionName=\"Cell_Across_Swath_1km\"\n" +
                "\t\t\t\tSize=1354\n" +
                "\t\t\tEND_OBJECT=Dimension_1\n" +
                "\t\t\tOBJECT=Dimension_2\n" +
                "\t\t\t\tDimensionName=\"Cell_Across_Swath_5km\"\n" +
                "\t\t\t\tSize=270\n" +
                "\t\t\tEND_OBJECT=Dimension_2\n" +
                "\t\t\tOBJECT=Dimension_3\n" +
                "\t\t\t\tDimensionName=\"Cell_Along_Swath_1km\"\n" +
                "\t\t\t\tSize=2030\n" +
                "\t\t\tEND_OBJECT=Dimension_3\n" +
                "\t\t\tOBJECT=Dimension_4\n" +
                "\t\t\t\tDimensionName=\"Cell_Along_Swath_5km\"\n" +
                "\t\t\t\tSize=406\n" +
                "\t\t\tEND_OBJECT=Dimension_4\n" +
                "\t\t\tOBJECT=Dimension_5\n" +
                "\t\t\t\tDimensionName=\"Byte_Segment\"\n" +
                "\t\t\t\tSize=6\n" +
                "\t\t\tEND_OBJECT=Dimension_5\n" +
                "\t\t\tOBJECT=Dimension_6\n" +
                "\t\t\t\tDimensionName=\"QA_Dimension\"\n" +
                "\t\t\t\tSize=10\n" +
                "\t\t\tEND_OBJECT=Dimension_6\n" +
                "\t\t\tOBJECT=Dimension_7\n" +
                "\t\t\t\tDimensionName=\"SPI_nband\"\n" +
                "\t\t\t\tSize=2\n" +
                "\t\t\tEND_OBJECT=Dimension_7\n" +
                "\t\tEND_GROUP=Dimension\n" +
                "\t\tGROUP=DimensionMap\n" +
                "\t\t\tOBJECT=DimensionMap_1\n" +
                "\t\t\t\tGeoDimension=\"Cell_Across_Swath_5km\"\n" +
                "\t\t\t\tDataDimension=\"Cell_Across_Swath_1km\"\n" +
                "\t\t\t\tOffset=2\n" +
                "\t\t\t\tIncrement=5\n" +
                "\t\t\tEND_OBJECT=DimensionMap_1\n" +
                "\t\t\tOBJECT=DimensionMap_2\n" +
                "\t\t\t\tGeoDimension=\"Cell_Along_Swath_5km\"\n" +
                "\t\t\t\tDataDimension=\"Cell_Along_Swath_1km\"\n" +
                "\t\t\t\tOffset=2\n" +
                "\t\t\t\tIncrement=5\n" +
                "\t\t\tEND_OBJECT=DimensionMap_2\n" +
                "\t\tEND_GROUP=DimensionMap\n" +
                "\t\tGROUP=IndexDimensionMap\n" +
                "\t\tEND_GROUP=IndexDimensionMap\n" +
                "\t\tGROUP=GeoField\n" +
                "\t\t\tOBJECT=GeoField_1\n" +
                "\t\t\t\tGeoFieldName=\"Latitude\"\n" +
                "\t\t\t\tDataType=DFNT_FLOAT32\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=GeoField_1\n" +
                "\t\t\tOBJECT=GeoField_2\n" +
                "\t\t\t\tGeoFieldName=\"Longitude\"\n" +
                "\t\t\t\tDataType=DFNT_FLOAT32\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=GeoField_2\n" +
                "\t\tEND_GROUP=GeoField\n" +
                "\t\tGROUP=DataField\n" +
                "\t\t\tOBJECT=DataField_1\n" +
                "\t\t\t\tDataFieldName=\"Byte_Segment\"\n" +
                "\t\t\t\tDataType=DFNT_INT32\n" +
                "\t\t\t\tDimList=(\"Byte_Segment\")\n" +
                "\t\t\tEND_OBJECT=DataField_1\n" +
                "\t\t\tOBJECT=DataField_2\n" +
                "\t\t\t\tDataFieldName=\"Scan_Start_Time\"\n" +
                "\t\t\t\tDataType=DFNT_FLOAT64\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=DataField_2\n" +
                "\t\t\tOBJECT=DataField_3\n" +
                "\t\t\t\tDataFieldName=\"Solar_Zenith\"\n" +
                "\t\t\t\tDataType=DFNT_INT16\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=DataField_3\n" +
                "\t\t\tOBJECT=DataField_4\n" +
                "\t\t\t\tDataFieldName=\"Solar_Azimuth\"\n" +
                "\t\t\t\tDataType=DFNT_INT16\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=DataField_4\n" +
                "\t\t\tOBJECT=DataField_5\n" +
                "\t\t\t\tDataFieldName=\"Sensor_Zenith\"\n" +
                "\t\t\t\tDataType=DFNT_INT16\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=DataField_5\n" +
                "\t\t\tOBJECT=DataField_6\n" +
                "\t\t\t\tDataFieldName=\"Sensor_Azimuth\"\n" +
                "\t\t\t\tDataType=DFNT_INT16\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_5km\",\"Cell_Across_Swath_5km\")\n" +
                "\t\t\tEND_OBJECT=DataField_6\n" +
                "\t\t\tOBJECT=DataField_7\n" +
                "\t\t\t\tDataFieldName=\"Cloud_Mask_SPI\"\n" +
                "\t\t\t\tDataType=DFNT_INT16\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_1km\",\"Cell_Across_Swath_1km\",\"SPI_nband\")\n" +
                "\t\t\tEND_OBJECT=DataField_7\n" +
                "\t\t\tOBJECT=DataField_8\n" +
                "\t\t\t\tDataFieldName=\"Cloud_Mask\"\n" +
                "\t\t\t\tDataType=DFNT_INT8\n" +
                "\t\t\t\tDimList=(\"Byte_Segment\",\"Cell_Along_Swath_1km\",\"Cell_Across_Swath_1km\")\n" +
                "\t\t\tEND_OBJECT=DataField_8\n" +
                "\t\t\tOBJECT=DataField_9\n" +
                "\t\t\t\tDataFieldName=\"Quality_Assurance\"\n" +
                "\t\t\t\tDataType=DFNT_INT8\n" +
                "\t\t\t\tDimList=(\"Cell_Along_Swath_1km\",\"Cell_Across_Swath_1km\",\"QA_Dimension\")\n" +
                "\t\t\tEND_OBJECT=DataField_9\n" +
                "\t\tEND_GROUP=DataField\n" +
                "\t\tGROUP=MergedFields\n" +
                "\t\tEND_GROUP=MergedFields\n" +
                "\tEND_GROUP=SWATH_1\n" +
                "END_GROUP=SwathStructure\n" +
                "GROUP=GridStructure\n" +
                "END_GROUP=GridStructure\n" +
                "GROUP=PointStructure\n" +
                "END_GROUP=PointStructure\n" +
                "END";

        assertEquals(1354, ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, "Cell_Across_Swath_1km"));
        assertEquals(270, ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, "Cell_Across_Swath_5km"));
        assertEquals(2030, ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, "Cell_Along_Swath_1km"));
        assertEquals(406, ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, "Cell_Along_Swath_5km"));
        assertEquals(6, ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, "Byte_Segment"));
        assertEquals(10, ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, "QA_Dimension"));
    }

    @Test
    public void testSetStartStopTimes() {
        final int year = 2011;
        final int doy = 196;
        final int hour = 10;
        final int min = 55;
        final int sec = 0;
        Product p = new Product("test", "test", 1, 1);

        final ProductData.UTC utc = ModisMod35L2Utils.getProductDate(year, doy, hour, min, sec);
        assertNotNull(utc);
        assertEquals("15-JUL-2011 10:55:00.000000", utc.format());
        p.setStartTime(utc);
        p.setEndTime(utc);
        System.out.println("p.getStartTime() = " + p.getStartTime());
        System.out.println("p.getEndTime() = " + p.getEndTime());
    }
}
