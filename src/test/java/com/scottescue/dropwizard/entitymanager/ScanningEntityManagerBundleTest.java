package com.scottescue.dropwizard.entitymanager;

import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.FakeEntity1;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.FakeEntity2;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.deep.DeepFakeEntity;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.deep.deeper.DeeperFakeEntity;
import io.dropwizard.Configuration;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanningEntityManagerBundleTest {
    @Test
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
                FakeEntity2.class,
                DeepFakeEntity.class,
                DeeperFakeEntity.class);
    }
}