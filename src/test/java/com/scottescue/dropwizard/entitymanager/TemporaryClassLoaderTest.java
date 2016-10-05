package com.scottescue.dropwizard.entitymanager;

import com.scottescue.dropwizard.entitymanager.entity.Person;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


public class TemporaryClassLoaderTest {

    private final TemporaryClassLoader.ClassLoaderOperations operations =
            mock(TemporaryClassLoader.ClassLoaderOperations.class);

    private final TemporaryClassLoader temporaryClassLoader = new TemporaryClassLoader(
            Thread.currentThread().getContextClassLoader(),
            operations);

    @Before
    public void setup() {
        when(operations.defineClass(anyString(), any())).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        });

        when(operations.getResourceAsStream(anyString())).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        });
    }

    @Test
    public void allowedClassIsNotResolveWhenNotAsked() throws Exception {
        temporaryClassLoader.loadClass(Person.class.getName(), false);

        verify(operations, never()).resolveClass(any());
    }

    @Test
    public void allowedClassIsResolvedWhenAsked() throws Exception {
        temporaryClassLoader.loadClass(Person.class.getName(), true);

        verify(operations).resolveClass(any());
    }

    @Test
    public void allowedClassIsNotLoadedByParent() throws Exception {
        temporaryClassLoader.loadClass(Person.class.getName());

        verify(operations, never()).loadClass(eq(Person.class.getName()), anyBoolean());
    }

    @Test
    public void excludedClassLoadedByParent() throws Exception {
        temporaryClassLoader.loadClass("java.lang.Object");

        verify(operations).loadClass(eq("java.lang.Object"), anyBoolean());
    }

    @Test
    public void classIsCorrectlyLoadedFromRealInstance() throws Exception {
        TemporaryClassLoader temporaryClassLoader = new TemporaryClassLoader(Thread.currentThread().getContextClassLoader());
        Class<?> type = temporaryClassLoader.loadClass(Person.class.getName());

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo(Person.class.getName());
    }

    @Test
    public void classIsCorrectlyLoadedAndResolvedFromRealInstance() throws Exception {
        TemporaryClassLoader temporaryClassLoader = new TemporaryClassLoader(Thread.currentThread().getContextClassLoader());
        Class<?> type = temporaryClassLoader.loadClass(Person.class.getName(), true);

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo(Person.class.getName());
    }

    @Test
    public void classesAreCorrectlyExcluded() {
        String[] expectedExclusions = new String[] {
                "java.lang.Object",
                "javafx.application.Application",
                "javax.crypto.Cipher",
                "oracle.jrockit.jfr.DCmd",
                "org.omg.CosNaming.Binding",
                "org.w3c.dom.Entity",
                "org.xml.sax.Parser",
                "sun.security.pkcs11.KeyCache",
                "javassist.ClassPool",
                "net.bytebuddy.ByteBuddy",
                "org.apache.commons.logging.Log",
                "org.apache.log4j.Logger",
                "org.eclipse.jetty.http.HttpURI",
                "org.glassfish.jersey.server.ApplicationHandler",
                "org.slf4j.Logger"
        };
        for (String className : expectedExclusions) {
            assertThat(temporaryClassLoader.isExcluded(className))
                    .describedAs("Expected \"%s\" to be an excluded class", className)
                    .isTrue();
        }
    }

    @Test
    public void nullStreamCausesClassNotFound() throws Exception {
        when(operations.getResourceAsStream(anyString())).thenReturn(null);

        assertThatThrownBy(() -> temporaryClassLoader.loadClass(Person.class.getName(), false))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining(Person.class.getName());
    }

    @Test
    public void errorReadingResourceCausesClassNotFound() throws Exception {
        doThrow(IOException.class).when(operations).copyStreams(any(), any());

        assertThatThrownBy(() -> temporaryClassLoader.loadClass(Person.class.getName()))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining(Person.class.getName());
    }

}