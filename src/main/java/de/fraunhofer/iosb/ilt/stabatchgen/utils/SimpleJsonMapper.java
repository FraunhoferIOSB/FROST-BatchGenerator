/*
 * Copyright (C) 2026 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.stabatchgen.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TreeNode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;

/**
 * A simple JSON Mapper for non-STA use.
 */
public class SimpleJsonMapper {

    private static final String FAILED_JSON_PARSE = "Failed to parse stored json.";

    private static ObjectMapper simpleObjectMapper;
    private static ObjectMapper simplePrettyMapper;

    private SimpleJsonMapper() {
        // Utility class.
    }

    /**
     * get an ObjectMapper for generic, non-STA use.
     *
     * @return an ObjectMapper for generic, non-STA use.
     */
    public static ObjectMapper getObjectMapper() {
        if (simpleObjectMapper == null) {
            simpleObjectMapper = JsonMapper.builder()
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                    .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_EMPTY))
                    .disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
                    .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                    .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .build();
        }
        return simpleObjectMapper;
    }

    public static ObjectMapper getPrettyMapper() {
        if (simplePrettyMapper == null) {
            simplePrettyMapper = getObjectMapper()
                    .rebuild()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();
        }
        return simplePrettyMapper;
    }

    public static JsonNode valueToTree(Object value) {
        return getObjectMapper().valueToTree(value);
    }

    public static JsonNode jsonToTreeOrString(String json) {
        if (json == null) {
            return null;
        }

        try {
            return getObjectMapper().readTree(json);
        } catch (JacksonException ex) {
            return new StringNode(json);
        }
    }

    public static JsonNode jsonToTree(String json) {
        if (json == null) {
            return null;
        }

        try {
            return getObjectMapper().readTree(json);
        } catch (JacksonException ex) {
            throw new IllegalStateException(FAILED_JSON_PARSE, ex);
        }
    }

    public static <T> T treeToObject(TreeNode json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return getObjectMapper().treeToValue(json, clazz);
        } catch (JacksonException ex) {
            throw new IllegalStateException(FAILED_JSON_PARSE, ex);
        }
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JacksonException ex) {
            throw new IllegalStateException(FAILED_JSON_PARSE, ex);
        }
    }

    public static <T> T jsonToObject(String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, typeReference);
        } catch (JacksonException ex) {
            throw new IllegalStateException(FAILED_JSON_PARSE, ex);
        }
    }
}
