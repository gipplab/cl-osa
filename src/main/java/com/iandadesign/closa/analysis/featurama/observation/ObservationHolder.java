package com.iandadesign.closa.analysis.featurama.observation;
import java.util.*;

public class ObservationHolder {

    public ArrayList<Observation> observations = new ArrayList<>();
    public ArrayList<String> dataNames = new ArrayList<>();

/* ------------------------
   Constructor
 * ------------------------ */

    public ObservationHolder()
    {
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    public ObservationHolder(int size)
    {
        this.reserve(size);
    }

    public void reserve(int size)
    {
        this.observations.ensureCapacity(size);
    }

    public int size()
    {
        return observations.size();
    }

    public void add(Observation obs)
    {
        this.observations.add(obs);
        // update List Names
        List<String> obsKeys = new ArrayList<>(obs.features.keySet());
        obsKeys.removeAll(this.dataNames);
        this.dataNames.addAll(obsKeys);
    }


}
