/*
 * Copyright © 2020-2022 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.securebanking.uk.gateway.conversion.converters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic converter to provide a common way conversion between json string and OB object types
 * @param <T> represents OB data model object
 */
public class GenericIntentConverter<T> {
    private final Function<String, T> fromJsonString;

    public GenericIntentConverter(final Function<String, T> fromJsonString) {
        this.fromJsonString = fromJsonString;
    }

    public final T convertFromJsonString(final String jsonString) {
        return (T) fromJsonString.apply(jsonString);
    }

    public final List<T> createFromJsonStrings(final Collection<String> jsonStrings) {
        return jsonStrings.stream().map(this::convertFromJsonString).collect(Collectors.toList());
    }

}
