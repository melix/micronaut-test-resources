/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.test.extensions.testresources;

import io.micronaut.testresources.client.TestResourcesClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeTestResourcesClient implements TestResourcesClient {
    private static final Map<String, String> MOCK_PROPERTIES = Map.of(
            "first-property", "first supplied by test resources",
            "second-property", "second supplied by test resources",
            "some-property", "supplied by test resources",
            "property-with-requirements", "supplied by test resources with requirements"
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return MOCK_PROPERTIES.keySet().stream().toList();
    }

    @Override
    public Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        var value = MOCK_PROPERTIES.get(name);
        if ("property-with-requirements".equals(name)) {
            if (!properties.containsKey("required-property")) {
                return Optional.empty();
            } else {
                value += ": " + properties.get("required-property");
            }
        }
        return Optional.ofNullable(value);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if ("property-with-requirements".equals(expression)) {
            return List.of("required-property");
        }
        return List.of();
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of();
    }

    @Override
    public boolean closeAll() {
        return true;
    }

    @Override
    public boolean closeScope(String id) {
        return true;
    }
}
