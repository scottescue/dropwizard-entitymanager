package com.scottescue.dropwizard.entitymanager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of {@link EntityManagerBundle} that scans a given package for entities instead of requiring entities
 * to be explicitly listed.
 *
 * @param <T> the {@link Configuration} type expected by this bundle
 */
public abstract class ScanningEntityManagerBundle<T extends Configuration> extends EntityManagerBundle<T> {
    /**
     * @param path string with package containing JPA entities (classes annotated with {@code @Entity}
     *             annotation) e.g. {@code com.my.application.directory.entities}
     */
    protected ScanningEntityManagerBundle(String path) {
        this(path,
                new EntityManagerFactoryFactory(),
                new SharedEntityManagerFactory());
    }

    @VisibleForTesting
    ScanningEntityManagerBundle(String path,
                                          EntityManagerFactoryFactory entityManagerFactoryFactory,
                                          SharedEntityManagerFactory sharedEntityManagerFactory) {
        super(findEntityClassesFromDirectory(path), entityManagerFactoryFactory, sharedEntityManagerFactory);
    }

    private static ImmutableList<Class<?>> findEntityClassesFromDirectory(String path) {
        @SuppressWarnings("unchecked")
        final AnnotationAcceptingListener asl = new AnnotationAcceptingListener(Entity.class);
        final PackageNamesScanner scanner = new PackageNamesScanner(new String[]{path}, true);

        while (scanner.hasNext()) {
            final String next = scanner.next();
            if (asl.accept(next)) {
                try (final InputStream in = scanner.open()) {
                    asl.process(next, in);
                } catch (IOException e) {
                    throw new RuntimeException("AnnotationAcceptingListener failed to process scanned resource: " + next);
                }
            }
        }

        final ImmutableList.Builder<Class<?>> builder = ImmutableList.builder();
        asl.getAnnotatedClasses().forEach(builder::add);

        return builder.build();
    }
}
