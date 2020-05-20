package com.iandadesign.closa.model;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Additional Handler to serialized SavedEntity,
 * required for unblocking ObjectOutputStream.
 *
 * @author Johannes Stegmüller
 */
public class SavedEntitiesObjectOutputStream extends ObjectOutputStream {

    /**
     * @param out outputStream used
     * @throws IOException if problem with output stream
     */
    public SavedEntitiesObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }
}