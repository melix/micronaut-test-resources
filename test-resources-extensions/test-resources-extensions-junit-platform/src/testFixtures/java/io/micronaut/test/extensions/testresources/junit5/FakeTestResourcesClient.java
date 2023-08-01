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
package io.micronaut.test.extensions.testresources.junit5;

import io.micronaut.testresources.client.TestResourcesClient;
import io.micronaut.testresources.codec.Result;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeTestResourcesClient implements TestResourcesClient {
    private static final Map<String, String> MOCK_PROPERTIES = Map.of(
            "some-property", "supplied by test resources"
    );

    private static final ThreadLocal<Set<String>> CLOSED_SCOPES = ThreadLocal.withInitial(HashSet::new);

    @Override
    public Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Result.of(MOCK_PROPERTIES.keySet().stream().toList());
    }

    @Override
    public Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        return Result.asOptional(MOCK_PROPERTIES.get(name));
    }

    @Override
    public Result<List<String>> getRequiredProperties(String expression) {
        return Result.emptyList();
    }

    @Override
    public Result<List<String>> getRequiredPropertyEntries() {
        return Result.emptyList();
    }

    @Override
    public Result<Boolean> closeAll() {
        return Result.TRUE;
    }

    @Override
    public Result<Boolean> closeScope(String id) {
        CLOSED_SCOPES.get().add(id);
        return Result.TRUE;
    }

    public static Set<String> closedScopes() {
        return CLOSED_SCOPES.get();
    }

    public static void reset() {
        CLOSED_SCOPES.remove();
    }
}
