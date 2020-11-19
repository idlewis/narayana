/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.lra.client.internal.proxy.nonjaxrs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;

import io.narayana.lra.logging.LRALogger;

/**
 * This CDI extension collects all LRA participants that contain
 * one or more non-JAX-RS participant methods. The collected classes are stored
 * in {@link LRAParticipantRegistry}.
 */
public class LRACDIExtension implements Extension {

    private final Map<String, LRAParticipant> participants = new HashMap<>();
    private LraClassFinder classFinder;
    {
        ServiceLoader<LraClassFinder> finderLoader = ServiceLoader.load(LraClassFinder.class, this.getClass().getClassLoader());
        classFinder = finderLoader.findFirst().orElseGet(() -> new NarayanaClassFinder());
    }

    public void observe(@Observes AfterBeanDiscovery event, BeanManager beanManager) throws IOException, ClassNotFoundException {
        System.err.println("LRAD: observing in narayana extension");
        System.err.flush();

        Set<Class<?>> classes = classFinder.getClasses();
        for (Class<?> javaClass : classes) {
        //return participant.hasNonJaxRsMethods() ? participant : null;
            LRAParticipant participant = new LRAParticipant(javaClass);
            if (!participant.hasNonJaxRsMethods()) {
                continue;
            }
            participants.put(participant.getJavaClass().getName(), participant);
            Set<Bean<?>> participantBeans = beanManager.getBeans(participant.getJavaClass(), new AnnotationLiteral<Any>() {});
            if (participantBeans.isEmpty()) {
                // resource is not registered as managed bean so register a custom managed instance
                try {
                    participant.setInstance(participant.getJavaClass().newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    LRALogger.i18NLogger.error_cannotProcessParticipant(e);
                }
            }


        }

        event.addBean()
            .read(beanManager.createAnnotatedType(LRAParticipantRegistry.class))
            .beanClass(LRAParticipantRegistry.class)
            .scope(ApplicationScoped.class)
            .createWith(context -> new LRAParticipantRegistry(participants));
    }

}
