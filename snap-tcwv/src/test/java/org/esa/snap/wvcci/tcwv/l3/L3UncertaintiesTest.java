package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.Float.NaN;
import static java.lang.Math.sqrt;
import static org.esa.snap.wvcci.tcwv.l3.AggregatorTestUtils.*;
import static org.junit.Assert.assertEquals;

public class L3UncertaintiesTest {

    private BinContext ctx;

    @Before
    public void setUp() {
        ctx = createCtx();
    }


    @Test
    public void testAggregatorAverageWvcciL3_2() {
        AggregatorAverage agg = new AggregatorAverage(new MyVariableContext("c"), "c", "c", 1.0, true, false);

        VectorImpl tvec = vec(NaN, NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(12.0f, 144.0f, 3), 3, tvec);
        agg.aggregateTemporal(ctx, vec(10.0f, 100.0f, 2), 2, tvec);
        agg.aggregateTemporal(ctx, vec(8.0f, 64.0f, 3), 3, tvec);
        float sum = 12.0f * 3 + 10.0f * 2 + 8.0f * 3;
        float sumSqr = 144.0f * 3 + 100.0f * 2 + 64.0f * 3;
        float weightSum = 3 + 2 + 3;
        int counts = 3 + 2 + 3;
        assertEquals(sum, tvec.get(0), 1e-5f);
        assertEquals(sumSqr, tvec.get(1), 1e-5f);
        assertEquals(weightSum, tvec.get(2), 1e-5f);
        assertEquals(counts, tvec.get(2), 1e-5f);

        System.out.println("sum = " + sum);
        System.out.println("sumSqr = " + sumSqr);
        System.out.println("weightSum = " + weightSum);
        System.out.println("counts = " + counts);

        float mean = sum / weightSum;
        float sigma = (float) sqrt(sumSqr / weightSum - mean * mean);
        agg.computeOutput(tvec, out);
        System.out.println("mean = " + mean);
        System.out.println("sigma = " + sigma);
        System.out.println("counts = " + counts);
        assertEquals(mean, out.get(0), 1e-5f);
        assertEquals(sigma, out.get(1), 1e-5f);
        assertEquals(counts, out.get(2), 1e-5f);

        // expected sigma_SD_sqr from eq. (1) Cloucd CCI paper:
        float sigma_SD_sqr_expected = ((12.0f - mean) * (12.0f - mean) * 3 +
                (10.0f - mean) * (10.0f - mean) * 2 + (8.0f - mean) * (8.0f - mean) * 3) / counts;
        System.out.println("sigma_SD_sqr_expected = " + sigma_SD_sqr_expected);

        // use aggregated quantities instead of single samples to derive sigma_SD_sqr:
        float sigma_SD_sqr = (float) (sumSqr / counts - 2.0 * mean * sum / counts + mean * mean);
        System.out.println("sigma_SD_sqr = " + sigma_SD_sqr);
        assertEquals(sigma_SD_sqr, sigma_SD_sqr_expected, 1e-5f);

        // assume TCWV uncertainties of 5%:
        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.6f, 0.36f, 3), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.5f, 0.25f, 2), 2, tvec);
        agg.aggregateTemporal(ctx, vec(0.4f, 0.16f, 3), 3, tvec);
        float sumUnc = 0.6f * 3 + 0.5f * 2 + 0.4f * 3;
        float sumSqrUnc = 0.36f * 3 + 0.25f * 2 + 0.16f * 3;

        System.out.println("sumUnc = " + sumUnc);
        System.out.println("sumSqrUnc = " + sumSqrUnc);

        float meanUnc = sumUnc / weightSum;
        float sigmaUnc = (float) sqrt(sumSqrUnc / weightSum - meanUnc * meanUnc);
        agg.computeOutput(tvec, out);
        System.out.println("meanUnc = " + meanUnc);
        System.out.println("sigmaUnc = " + sigmaUnc);
        assertEquals(meanUnc, out.get(0), 1e-5f);
        assertEquals(sigmaUnc, out.get(1), 1e-5f);

        // eq. (4):
        float c = 1.0f;     // make this a user option, default 0.5
        float sigma_TRUE_sqr = sigma_SD_sqr - (1.0f - c) * sumSqrUnc / counts;
        System.out.println("sigma_TRUE_sqr = " + sigma_TRUE_sqr);

        // eq. (5):
        float sigma_FINAL_sqr = sigma_TRUE_sqr/counts + c*meanUnc*meanUnc + (1.0f - c) * sumSqrUnc / (counts*counts);
        System.out.println("sigma_FINAL_sqr = " + sigma_FINAL_sqr);
    }
}
