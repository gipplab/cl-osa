package com.iandadesign.closa.evaluation.featurama.observation;

import com.iandadesign.closa.util.ExtendedLogUtil;
import java.util.*;
import java.lang.reflect.*;

public class Observation {

    public LinkedHashMap<String, Object> observations;

    public Observation()
    {
        observations = new LinkedHashMap<>();
    }

    public Observation(LinkedHashMap<String, Object> observations)
    {
        this.observations = observations;
    }

    public void addData(LinkedHashMap<String, Object> observations)
    {
        this.observations = observations;
    }

    public void addData(Object ...observations)
    {
        for (Object observation : observations)
        {
            if (Objects.isNull(observation)) {
                continue;
            }
            process(observation, "");
        }
    }

    public void addData(Object observation, String prefix)
    {
        if (Objects.isNull(observation)) {
            return;
        }
        process(observation, prefix);
    }

    private void process(Object observation, String prefix)
    {
        for (Field field : observation.getClass().getDeclaredFields()) {
            field.setAccessible(true); // You might want to set modifier to public first.
            Object value = null;
            String type = "";
            try
            {
                value = field.get(observation);
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
            observations.put(prefix + name, value);
        }

    }

    public int returnObservationDim()
    {
        return this.observations.size();
    }

    public double[] returnObservationData(ArrayList<String> dataNames)
    {
        double[] returnArray = new double[dataNames.size()];
        for(int i = 0; i< dataNames.size(); i++)
        {
            if(this.observations.containsKey(dataNames.get(i)))
            {
                returnArray[i] = transformtodouble(this.observations.get(dataNames.get(i)));
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
}

