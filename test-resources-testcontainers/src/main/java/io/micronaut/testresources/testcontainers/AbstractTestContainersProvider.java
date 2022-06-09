/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.testcontainers;

import io.micronaut.testresources.core.TestResourcesResolver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.testresources.testcontainers.TestContainerMetadataSupport.SPECIFIC_ORDER;

/**
 * The base class for test resources providers which spawn test containers.
 *
 * @param <T> the container type
 */
public abstract class AbstractTestContainersProvider<T extends GenericContainer<? extends T>> implements TestResourcesResolver {
    @Override
    public int getOrder() {
        return SPECIFIC_ORDER;
    }

    /**
     * Returns the name of the resource resolver, for example "kafka" or "mysql".
     *
     * @return the name of the resolver
     */
    protected abstract String getSimpleName();

    /**
     * Returns the default image name.
     *
     * @return the default image name.
     */
    protected abstract String getDefaultImageName();

    /**
     * Creates the test container.
     *
     * @param imageName the docker image name
     * @param properties the resolved properties
     * @return a container
     */
    protected abstract T createContainer(DockerImageName imageName, Map<String, Object> properties);

    /**
     * Determines if this resolver can resolve the requested property.
     * It is used in order to make sure that a "Postgres" resolver wouldn't
     * provide a value if the requested container type is for MySQL, for
     * example.
     *
     * @param propertyName the property to resolve
     * @param properties the resolved properties
     * @return if this resolver should answer
     */
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties) {
        return true;
    }

    /**
     * Lets a resolver provide a value for the requested property without triggering the
     * creation of a test container. This can be used in case a resolver wants to check
     * existing containers first.
     * @param propertyName the name of the property to resolve
     * @param properties the properties used to resolve
     * @return a resolved property
     */
    protected Optional<String> resolveWithoutContainer(String propertyName, Map<String, Object> properties) {
        return Optional.empty();
    }

    @Override
    public final Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        if (shouldAnswer(propertyName, properties)) {
            Optional<String> firstPass = resolveWithoutContainer(propertyName, properties);
            if (firstPass.isPresent()) {
                return firstPass;
            }
            return resolveProperty(propertyName,
                TestContainers.getOrCreate(propertyName, this.getClass(), getSimpleName(), properties, () -> {
                    String defaultImageName = getDefaultImageName();
                    DockerImageName imageName = DockerImageName.parse(defaultImageName);
                    Optional<TestContainerMetadata> metadata = TestContainerMetadataSupport.containerMetadataFor(Collections.singletonList(getSimpleName()), testResourcesConfiguration)
                        .findAny();
                    if (metadata.isPresent()) {
                        TestContainerMetadata md = metadata.get();
                        if (md.getImageName() != null) {
                            imageName = DockerImageName.parse(md.getImageName()).asCompatibleSubstituteFor(defaultImageName);
                        }
                    }
                    return createContainer(imageName, properties);
                }));
        }
        return Optional.empty();
    }

    protected abstract Optional<String> resolveProperty(String propertyName, T container);

}
