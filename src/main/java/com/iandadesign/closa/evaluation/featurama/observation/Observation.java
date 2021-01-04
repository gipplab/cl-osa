package com.iandadesign.closa.evaluation.featurama.observation;

import java.util.*;
import java.lang.reflect.*;

public class Observation {

    public LinkedHashMap<String, Object> features;

/* ------------------------
   Constructor
 * ------------------------ */

    public Observation()
    {
        this.features = new LinkedHashMap<>();
    }

    public Observation(LinkedHashMap<String, Object> features)
    {
        this.features = features;
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    public void addData(LinkedHashMap<String, Object> features)
    {
        this.features = features;
    }

    public void addData(Object ...features)
    {
        for (Object feature : features)
        {
            if (Objects.isNull(feature)) {
                continue;
            }
            process(feature, "");
        }
    }

    public void addData(Object feature, String prefix)
    {
        if (Objects.isNull(feature)) {
            return;
        }
        process(feature, prefix);
    }

    public int returnObservationDim()
    {
        return this.features.size();
    }

    public double[] returnObservationData(ArrayList<String> dataNames)
    {
        double[] returnArray = new double[dataNames.size()];
        for(int i = 0; i< dataNames.size(); i++)
        {
            if(this.features.containsKey(dataNames.get(i)))
            {
                returnArray[i] = transformtodouble(this.features.get(dataNames.get(i)));
            }
            else
            {
                returnArray[i] = 0.0;
            }
        }
        return returnArray;
    }

    public double transformtodouble(Object input) {
        if(input instanceof Boolean)
        {
            boolean di = (Boolean) input;
            return di ? 1.0d : 0.0d;
        }
        Double returnObject = new Double(input.toString());
        return returnObject.doubleValue();
    }

/* ------------------------
   Private Methods
 * ------------------------ */

    private void process(Object feature, String prefix)
    {
        for (Field field : feature.getClass().getDeclaredFields()) {
            field.setAccessible(true); // You might want to set modifier to public first.
            Object value = null;
            String type = "";
            try
            {
                value = field.get(feature);
                type = field.getType().toString();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            if (value != null) {
                processFieldEntry(field.getName(), type, value, prefix);
            }
        }
    }

    private void processFieldEntry(String name, String type, Object value, String prefix)
    {
        if ((value instanceof Number) || (value instanceof Boolean))
        {
            this.features.put(prefix + name, value);
        }

    }
}

