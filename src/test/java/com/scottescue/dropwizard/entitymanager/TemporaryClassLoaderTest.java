package com.scottescue.dropwizard.entitymanager;

import com.scottescue.dropwizard.entitymanager.entity.Person;
import com.scottescue.dropwizard.entitymanager.entity.PersonType;
import com.scottescue.dropwizard.entitymanager.entity.Personable;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


public class TemporaryClassLoaderTest {

    private final ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
    private final TemporaryClassLoader temporaryClassLoader = spy(new TemporaryClassLoader(parentClassLoader));

    @Test
    public void unprotectedClassIsLoadedByTemporary() throws Exception {
        Class type = temporaryClassLoader.loadClass(Person.class.getName());
        assertThat(type.getClassLoader()).isSameAs(temporaryClassLoader);
    }

    @Test
    public void protectedClassLoadedByParent() throws Exception {
        Class type = temporaryClassLoader.loadClass(Object.class.getName());
        assertClassLoadedByParent(type);
    }

    @Test
    public void annotationClassLoadedByParent() throws Exception {
        Class type = temporaryClassLoader.loadClass(Personable.class.getName());
        assertClassLoadedByParent(type);
    }

    @Test
    public void enumClassLoadedByParent() throws Exception {
        Class type = temporaryClassLoader.loadClass(PersonType.class.getName());
        assertClassLoadedByParent(type);
    }

    @Test
    public void errorReadingResourceDefersToParent() throws Exception {
        doThrow(IOException.class).when(temporaryClassLoader).readBytes(any());

        Class type = temporaryClassLoader.loadClass(Person.class.getName());
        assertClassLoadedByParent(type);
    }

    @Test
    public void classIsNotLoadedTwice() throws Exception {
        Class first = temporaryClassLoader.loadClass(Person.class.getName());
        Class second = temporaryClassLoader.loadClass(Person.class.getName());

        assertThat(second).isSameAs(first);
    }

    @Test
    public void classIsLinkedWhenRequested() throws Exception {
        Class type = temporaryClassLoader.loadClass(Person.class.getName(), true);
        verify(temporaryClassLoader).resolve(type);
    }

    @Test
    public void classLoadedByParentDiffersFromTempClass() throws Exception {
        Class parentType = parentClassLoader.loadClass(Person.class.getName());
        Class tempType = temporaryClassLoader.loadClass(Person.class.getName());

        assertThat(tempType).isNotEqualTo(parentType);
    }

    @Test
    public void classesAreCorrectlyIdentifiedAsProtected() {
        String[] expectedExclusions = new String[] {
                "java.lang.Object",
                "javax.crypto.Cipher",
                "jdk.Exported",
                "sun.security.pkcs11.KeyCache",
                "oracle.jrockit.jfr.DCmd"
        };
        for (String className : expectedExclusions) {
            assertThat(temporaryClassLoader.isProtected(className))
                    .describedAs("Expected \"%s\" to be a protected class", className)
                    .isTrue();
        }
    }

    @Test
    public void nullStreamCausesClassNotFound() throws Exception {
        assertThatThrownBy(() -> temporaryClassLoader.loadClass("com.i.dont.Exist"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("com.i.dont.Exist");
    }

    private void assertClassLoadedByParent(Class<?> type) {
        // getClassLoader may return null if the class was loaded by the bootstrap classloader
        assertThat(type.getClassLoader()).isIn(null, parentClassLoader);
    }
}