package io.narayana.lra.client.internal.proxy.nonjaxrs;

import java.io.IOException;
import java.util.Set;

/**
 * An extension intended to be used with ServiceLoader to allow
 * user to provide a list of LRA application classes with non-JAX-RS
 * methods
 */
public interface LraClassFinder {

    /**
     * Should return a Set of classes that are:
     *   - real (not abstract or interfaces)
     *   - LRA participants
     * Should throw an IllegalStateException if any classes are found which are invalid
     * LRA participants, i.e. they have LRA annotations, but do not have one of either
     * a compensate or afterLRA annotation
     */
    Set<Class<?>> getClasses() throws IOException, ClassNotFoundException;

}
