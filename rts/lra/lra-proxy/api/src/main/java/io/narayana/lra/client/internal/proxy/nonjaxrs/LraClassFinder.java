package io.narayana.lra.client.internal.proxy.nonjaxrs;

import java.io.IOException;
import java.util.Set;

public interface LraClassFinder {

    Set<Class<?>> getClasses() throws IOException, ClassNotFoundException;

}
