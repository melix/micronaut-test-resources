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
package io.micronaut.testresources.client;

import io.micronaut.core.io.ResourceLoader;

import java.net.URL;
import java.util.Optional;

final class ConfigFinder {
    public static final String SYSTEM_PROP_PREFIX = "micronaut.test.resources";

    public static final String TEST_RESOURCES_PROPERTIES = "/test-resources.properties";

    private ConfigFinder() {

    }

    static Optional<URL> findConfiguration(ResourceLoader loader) {
        Optional<URL> resource = Optional.empty();
        if (loader != null) {
            resource = loader.getResource(TEST_RESOURCES_PROPERTIES);
        }
        if (!resource.isPresent()) {
            resource = Optional.ofNullable(ConfigFinder.class.getResource(TEST_RESOURCES_PROPERTIES));
        }
        return resource;
    }

    static String systemPropertyNameOf(String propertyName) {
        return SYSTEM_PROP_PREFIX + "." + propertyName;
    }
}
