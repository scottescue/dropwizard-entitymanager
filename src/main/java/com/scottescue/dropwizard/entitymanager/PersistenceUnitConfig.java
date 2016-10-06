package com.scottescue.dropwizard.entitymanager;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import java.net.URL;

/**
 * Provides a means to configure the persistence unit before it is created.
 */
public interface PersistenceUnitConfig {

    /**
     * Specifies whether classes in the root of the persistence unit that have not been explicitly listed are to
     * be included in the set of managed classes.  Classes that have not been explicitly listed are excluded by
     * default.
     *
     * @param excludeUnlistedClasses whether classes in the root of the persistence unit that have not been
     *                               explicitly listed are to be included in the set of managed classes
     * @return a reference to this object
     */
    PersistenceUnitConfig setExcludeUnlistedClasses(boolean excludeUnlistedClasses);

    /**
     * Specifies how the provider must use a second-level cache for the persistence unit.
     * {@link SharedCacheMode#UNSPECIFIED} is used by default.
     *
     * @param sharedCacheMode the second-level cache mode that must be used by the provider for the persistence unit
     * @return a reference to this object
     */
    PersistenceUnitConfig setSharedCacheMode(SharedCacheMode sharedCacheMode);

    /**
     * Specifies the validation mode to be used by the persistence provider for the persistence unit.
     * {@link ValidationMode#AUTO} is used by default.
     *
     * @param validationMode the validation mode to be used by the persistence provider for the persistence unit
     * @return a reference to this object
     */
    PersistenceUnitConfig setValidationMode(ValidationMode validationMode);

    /**
     * Specifies any mapping files that must be loaded to determine the mappings
     * for the entity classes. The mapping files must be in the standard XML mapping format, be
     * uniquely named and be resource-loadable from the application classpath. There are no mapping
     * files used by default.
     *
     * @param fileName mapping file name that the persistence provider must load to determine the
     *                 mappings for the entity classes
     * @param fileNames list of any additional mapping file names that the persistence provider must
     *                  load to determine the mappings for the entity classes
     * @return a reference to this object
     */
    PersistenceUnitConfig addMappingFileNames(String fileName, String... fileNames);

    /**
     * Specifies URLs for any jar files or exploded jar file directories that the persistence provider must
     * examine for managed classes of the persistence unit. A URL will either be a file: URL referring to a
     * jar file or referring to a directory that contains an exploded jar file, or some other URL from which
     * an InputStream in jar format can be obtained.
     *
     * @param url URL object referring to a jar file or directory
     * @param urls list of any additional URL objects referring to jar files or directories
     * @return a reference to this object
     */
    PersistenceUnitConfig addJarFileUrls(URL url, URL... urls);

    /**
     * Specifies a property value to be used by the persistence provider for the persistence unit.
     *
     * @param property property name
     * @param value the value to be used by the persistence provider for the persistence unit
     * @return a reference to this object
     */
    PersistenceUnitConfig setProperty(String property, String value);

}
