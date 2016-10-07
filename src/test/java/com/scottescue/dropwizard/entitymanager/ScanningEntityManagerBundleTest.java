package com.scottescue.dropwizard.entitymanager;

import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.FakeEntity1;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.deep.DeepFakeEntity;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.deep.deeper.DeeperFakeEntity;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg2.FakeEntity2;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg3.FakeEntity3;
import io.dropwizard.Configuration;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanningEntityManagerBundleTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testFindEntityClassesFromDirectory() {
        String packageWithEntities = "com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg";
        ScanningEntityManagerBundle bundle = new ScanningEntityManagerBundle(packageWithEntities) {
            @Override
            public void run(Object o, Environment environment) throws Exception {
            }

            @Override
            public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
                return null;
            }
        };

        assertThat(bundle.getEntities()).containsOnly(
                FakeEntity1.class,
                DeepFakeEntity.class,
                DeeperFakeEntity.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindEntityClassesFromMultipleDirectories() {
        String root = "com.scottescue.dropwizard.entitymanager.entity.fake.entities.";

        ScanningEntityManagerBundle bundle = new ScanningEntityManagerBundle(
                root.concat("pckg"), root.concat("pckg2"), root.concat("pckg3")) {
            @Override
            public void run(Object o, Environment environment) throws Exception {
            }

            @Override
            public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
                return null;
            }
        };

        assertThat(bundle.getEntities()).containsOnly(
                FakeEntity1.class,
                FakeEntity2.class,
                FakeEntity3.class,
                DeepFakeEntity.class,
                DeeperFakeEntity.class);
    }
}