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
package com.forgerock.securebanking.uk.gateway.conversion.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.forgerock.securebanking.uk.gateway.conversion.DateTimeDeserializerConverter;
import com.forgerock.securebanking.uk.gateway.conversion.DateTimeSerializerConverter;
import org.joda.time.DateTime;

public final class GenericConverterMapper {

    private static GenericConverterMapper instance;
    public ObjectMapper mapper;

    public GenericConverterMapper() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule customModule = new SimpleModule();
        customModule.addDeserializer(DateTime.class, new DateTimeDeserializerConverter());
        customModule.addSerializer(DateTime.class, new DateTimeSerializerConverter());
        mapper.registerModule(customModule);
    }

    public static ObjectMapper getMapper() {
        if (instance == null) {
            instance = new GenericConverterMapper();
        }
        return instance.mapper;
    }
}
