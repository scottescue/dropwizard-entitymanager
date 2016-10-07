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
     * @param paths any additional strings with packages containing JPA entities
     */
    protected ScanningEntityManagerBundle(String path, String... paths) {
        this(new EntityManagerFactoryFactory(), new SharedEntityManagerFactory(), path, paths);
    }

    @VisibleForTesting
    ScanningEntityManagerBundle(EntityManagerFactoryFactory entityManagerFactoryFactory,
                                SharedEntityManagerFactory sharedEntityManagerFactory,
                                String path, String... paths) {
        super(findEntityClassesFromDirectory(path, paths), entityManagerFactoryFactory, sharedEntityManagerFactory);
    }

    private static ImmutableList<Class<?>> findEntityClassesFromDirectory(String path, String... paths) {
        @SuppressWarnings("unchecked")
        final AnnotationAcceptingListener asl = new AnnotationAcceptingListener(Entity.class);
        final PackageNamesScanner scanner = new PackageNamesScanner(merge(path, paths), true);

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

    private static String[] merge(String arg, String... args) {
        String[] combinedPaths = new String[args.length + 1];
        combinedPaths[0] = arg;
        System.arraycopy(args, 0, combinedPaths, 1, args.length);
        return combinedPaths;
    }
}
