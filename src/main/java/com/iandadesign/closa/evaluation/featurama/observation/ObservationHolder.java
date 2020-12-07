package com.iandadesign.closa.evaluation.featurama.observation;
import java.util.*;

public class ObservationHolder {

    public ArrayList<Observation> observations = new ArrayList<>();
    public ArrayList<String> dataNames = new ArrayList<>();

    public ObservationHolder()
    {
    }

    public ObservationHolder(int size)
    {
        this.observations.ensureCapacity(size);
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
        List<String> obsKeys = new ArrayList<>(obs.observations.keySet());
        obsKeys.removeAll(this.dataNames);
        this.dataNames.addAll(obsKeys);
    }


}
