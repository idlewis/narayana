package io.narayana.lra.client.internal.proxy.nonjaxrs;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import io.narayana.lra.client.internal.proxy.nonjaxrs.jandex.DotNames;
import io.narayana.lra.client.internal.proxy.nonjaxrs.jandex.JandexAnnotationResolver;

public class NarayanaClassFinder implements LraClassFinder {

    private ClassPathIndexer classPathIndexer = new ClassPathIndexer();
    private Index index;

    @Override
    public Set<Class<?>> getClasses() throws IOException, ClassNotFoundException {

        Set<Class<?>> classes = new HashSet<>();

        index = classPathIndexer.createIndex();

        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple("javax.ws.rs.Path"));

        for (AnnotationInstance annotation : annotations) {
            ClassInfo classInfo;
            AnnotationTarget target = annotation.target();

            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                classInfo = target.asClass();
            } else if (target.kind().equals(AnnotationTarget.Kind.METHOD)) {
                classInfo = target.asMethod().declaringClass();
            } else {
                continue;
            }

            Class<?> javaClass = getClass().getClassLoader().loadClass(classInfo.name().toString());
            if (javaClass.isInterface() || Modifier.isAbstract(javaClass.getModifiers()) || !isLRAParticipant(classInfo)) {
                continue;
            }

            classes.add(javaClass);

        }


        return classes;
    }



    /**
     * Returns whether the classinfo represents an LRA participant --
     * Class contains LRA method and either one or both of Compensate and/or AfterLRA methods.
     *
     * @param classInfo Jandex class object to scan for annotations
     *
     * @return true if the class is a valid LRA participant, false otherwise
     * @throws IllegalStateException if there is LRA annotation but no Compensate or AfterLRA is found
     */
    private boolean isLRAParticipant(ClassInfo classInfo) {
        Map<DotName, List<AnnotationInstance>> annotations = JandexAnnotationResolver.getAllAnnotationsFromClassInfoHierarchy(classInfo.name(), index);

        if (!annotations.containsKey(DotNames.LRA)) {
            return false;
        } else if (!annotations.containsKey(DotNames.COMPENSATE) && !annotations.containsKey(DotNames.AFTER_LRA)) {
            throw new IllegalStateException(String.format("%s: %s",
                classInfo.name(), "The class contains an LRA method and no Compensate or AfterLRA method was found."));
        } else {
            return true;
        }
    }

}
