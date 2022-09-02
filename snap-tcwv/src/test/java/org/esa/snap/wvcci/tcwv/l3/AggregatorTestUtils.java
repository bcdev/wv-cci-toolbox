package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.support.ObservationImpl;
import org.esa.snap.binning.support.VectorImpl;

import java.util.HashMap;

public class AggregatorTestUtils {

    public static VectorImpl vec(float... values) {
        return new VectorImpl(values);
    }

    public static Observation obs(double mjd, float... values) {
        return new ObservationImpl(0.0, 0.0, mjd, values);
    }

    public static Observation obsNT(float... values) {
        return new ObservationImpl(0.0, 0.0, 0.0, values);
    }

    public static BinContext createCtx() {
        return new BinContext() {
            private HashMap<String, Object> map = new HashMap<String, Object>();

            @Override
            public long getIndex() {
                return 0;
            }

            @Override
            public <T> T get(String name) {
                return (T) map.get(name);
            }

            @Override
            public void put(String name, Object value) {
                map.put(name, value);
            }

            @Override
            public String ensureUnique(String name) {
                return name;
            }
        };
    }
}
