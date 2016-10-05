package com.scottescue.dropwizard.entitymanager;

import com.google.common.annotations.VisibleForTesting;
import org.hibernate.engine.jdbc.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class TemporaryClassLoader extends ClassLoader {

    // This exists solely for unit testing - unit tests can mock ClassLoaderOperations to verify interactions
    interface ClassLoaderOperations {
        void resolveClass(Class<?> type);

        Class<?> defineClass(String name, byte[] bytes) throws ClassFormatError;

        Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException;

        InputStream getResourceAsStream(String name);

        void copyStreams(InputStream inputStream, OutputStream outputStream) throws IOException;
    }

    // Classes from these packages should always be loaded using the parent class loader
    private static final String[] EXCLUDED_PACKAGES = new String[] {
            "java.",
            "javafx.",
            "javax.",
            "sun.",
            "oracle.",
            "org.omg.",
            "org.w3c.",
            "org.xml.",
            "javassist.",
            "net.bytebuddy.",
            "org.apache.",
            "org.eclipse.jetty.",
            "org.glassfish.",
            "org.slf4j."
    };

    static {
        ClassLoader.registerAsParallelCapable();
    }


    private final ClassLoaderOperations operations;


    /**
     * Expected to be the standard constructor used outside of unit tests.  Creates the default implementation
     * of ClassLoaderOperations.
     *
     * @param parent
     */
    TemporaryClassLoader(ClassLoader parent) {
        super(parent);
        this.operations = new ClassLoaderOperations() {
            @Override
            public void resolveClass(Class<?> type) {
                TemporaryClassLoader.this.resolveClass(type);
            }

            @Override
            public Class<?> defineClass(String name, byte[] bytes) throws ClassFormatError {
                return TemporaryClassLoader.this.defineClass(name, bytes, 0, bytes.length);
            }

            @Override
            public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                return TemporaryClassLoader.super.loadClass(name, resolve);
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                return TemporaryClassLoader.this.getResourceAsStream(name);
            }

            @Override
            public void copyStreams(InputStream inputStream, OutputStream outputStream) throws IOException {
                StreamUtils.copy(inputStream, outputStream, 4096);
            }
        };
    }

    /**
     * Expected to be used by unit tests that want to mock ClassLoaderOperations
     *
     * @param parent parent ClassLoader
     * @param operations ClassLoaderOperations implementation
     */
    TemporaryClassLoader(ClassLoader parent, ClassLoaderOperations operations) {
        super(parent);
        this.operations = operations;
    }


    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        // If the class does not belong to an excluded package, attempt to load it
        if (!isExcluded(className)) {
            synchronized (getClassLoadingLock(className)) {
                Class<?> result = findLoadedClass(className);
                if (result == null) {
                    result = findAndDefineClass(className);
                }
                if (resolve) {
                    operations.resolveClass(result);
                }
                return result;
            }
        } else {
            // The class belongs to an excluded package, so it should be loaded by the parent
            // The default implementation of this call delegates to super#loadClass, which establishes its own lock
            return operations.loadClass(className, resolve);
        }
    }

    @VisibleForTesting
    boolean isExcluded(String className) {
        for (String packageName : EXCLUDED_PACKAGES) {
            if (className.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> findAndDefineClass(String className) throws ClassNotFoundException {
        byte[] bytes = loadBytesForClass(className);
        if (bytes != null) {
            return operations.defineClass(className, bytes);
        }
        throw new ClassNotFoundException("Cannot load class " + className);
    }

    private byte[] loadBytesForClass(String className) throws ClassNotFoundException {
        String internalName = className.replace('.', '/') + ".class";
        try (
                InputStream inputStream = operations.getResourceAsStream(internalName);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ){
            if (inputStream == null) {
                return null;
            }
            // Load the raw bytes.
            operations.copyStreams(inputStream, outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + className + "]", ex);
        }
    }

}
