package com.iandadesign.closa.evaluation.featurama.observation;
import java.util.ArrayList;

public class ObservationHolder {

    public ArrayList<Observation> observations = new ArrayList<>();

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
    }

}
