package com.scottescue.dropwizard.entitymanager;

import com.google.common.annotations.VisibleForTesting;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.hibernate.engine.jdbc.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ClassLoader implementation that allows classes to be temporarily loaded and then thrown away.
 */
class TemporaryClassLoader extends ClassLoader {
    private static final String[] PROTECTED_PACKAGES =
            new String[] {"java", "javax", "jdk", "sun", "oracle", "ibm", "IBM"};

    private ClassPool classPool = new ClassPool();

    TemporaryClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // Check if this classloader has already loaded the class
        Class type = findLoadedClass(name);
        if (type != null) {
            return type;
        }

        // Defer to the parent if this is a protected class
        if (isProtected(name)) {
            return super.loadClass(name, resolve);
        }

        String resourceName = name.replace('.', '/') + ".class";
        CtClass ctClass = null;
        try (InputStream resource = getResourceAsStream(resourceName)){
            if (resource == null) {
                throw new ClassNotFoundException(name);
            }

            byte[] classBytes = readBytes(resource);
            classPool.insertClassPath(new ByteArrayClassPath(name, classBytes));
            ctClass = classPool.get(name);

            // Annotations and enums should be loaded by the parent classloader,
            // to avoid potential classloader issues with the JVM
            if (ctClass.isAnnotation() || ctClass.isEnum()) {
                return super.loadClass(name, resolve);
            }

            type = defineClass(name, classBytes, 0, classBytes.length);
            if (resolve) {
                resolve(type);
            }
            return type;
        } catch (IOException | NotFoundException | SecurityException e ) {
            // Defer to the parent
            return super.loadClass(name, resolve);
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    @VisibleForTesting
    boolean isProtected(String name) {
        for (String protectedPackage : PROTECTED_PACKAGES) {
            if (name.startsWith(protectedPackage.concat("."))) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamUtils.copy(in, out, 4096);
        return out.toByteArray();
    }

    @VisibleForTesting
    void resolve(Class<?> type) {
        resolveClass(type);
    }
}
