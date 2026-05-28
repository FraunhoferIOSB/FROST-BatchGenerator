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

import static de.fraunhofer.iosb.ilt.configurable.Utils.isNullOrEmpty;

import de.fraunhofer.iosb.ilt.stabatchgen.model.source.Tuple;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * Utility for filling templates by replacing placeholders with data from a data
 * source.
 */
public class TemplateUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateUtils.class.getName());
    private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\{([^|{}\"]+)(\\|([^}\"]*))?\\}");
    private static final String BAD_CHARS_REGEX = "[^a-zA-Z0-9_.:,;-]";
    private static final Pattern BAD_CHARS_PATTERN = Pattern.compile(BAD_CHARS_REGEX);

    private TemplateUtils() {
        // Utility class
    }

    public static String fillJsonTemplate(String template, Object source) {
        return fillTemplate(template, source, JSON);
    }

    public static String fillUrlTemplate(String template, Object source) {
        return fillTemplate(template, source, URL);
    }

    public static String fillPlainTemplate(String template, Object source) {
        return fillTemplate(template, source, NONE);
    }

    public static String fillTemplate(String template, Object source, Quoter quoter) {
        Matcher matcher = PLACE_HOLDER_PATTERN.matcher(template);
        matcher.reset();
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (matcher.find()) {
            int start = matcher.start();
            result.append(template.substring(pos, start));
            result.append(findMatch(matcher.group(1), matcher.group(3), source, quoter));
            pos = matcher.end();
        }
        result.append(template.substring(pos));
        return result.toString();
    }

    private static String findMatch(final String path, final String deflt, final Object source, final Quoter quoter) {
        boolean numeric = false;
        boolean outerQuotes = false;
        boolean clean = false;
        final String realPath;
        int idx = path.indexOf(':');
        if (idx >= 0) {
            realPath = path.substring(idx + 1);
            final String options = path.substring(0, idx);
            outerQuotes = options.contains("Q");
            clean = options.contains("C");
            numeric = options.contains("N");
        } else {
            realPath = path;
        }
        Object value = getPath(realPath, source);
        if (value == null) {
            return deflt;
        }
        if (value instanceof JsonNode jn) {
            if (!jn.isValueNode()) {
                return deflt;
            }
            if (jn.isNumber() && numeric) {
                if (jn.isIntegralNumber()) {
                    value = jn.asBigInteger();
                } else {
                    value = jn.asDecimal();
                }
            } else {
                value = jn.asString();
            }
        }
        if (value instanceof Map || value instanceof List) {
            return deflt;
        }
        if (isNullOrEmpty(Objects.toString(value, ""))) {
            return deflt;
        }
        if (clean) {
            value = String.valueOf(value).replaceAll(BAD_CHARS_REGEX, "_");
        }
        return quoter.quote(value, outerQuotes);
    }

    public static Object getPath(String path, Object source) {
        String[] parts = StringUtils.split(path, '/');
        Object value = source;
        for (String part : parts) {
            part = JsonUtils.DecodeJsonPointer(part);
            value = getField(part, value);
            if (value == null) {
                return null;
            }
        }
        return value;
    }

    public static Object getField(String field, Object source) {
        if (source instanceof Tuple t) {
            return t.get(field);
        } else if (source instanceof JsonNode jn) {
            if (jn.isObject()) {
                return jn.get(field);
            } else if (jn.isArray() && source instanceof Number n) {
                return jn.get(n.intValue());
            }
        } else if (source instanceof Map map) {
            return map.get(field);
        } else if (source instanceof List list) {
            try {
                Integer idx = Integer.valueOf(field);
                return list.get(idx);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        String getterName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
        try {
            return MethodUtils.invokeMethod(source, getterName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            LOGGER.trace("Failed to execute getter {} on {}", getterName, source, ex);
            return null;
        }
    }

    public static String getTemplateFrom(JsonNode templateHolder, String name, String dflt) {
        if (templateHolder == null) {
            return dflt;
        }
        JsonNode template = templateHolder.get(name);
        if (template == null || !template.isString()) {
            return dflt;
        }
        return template.asString();
    }

    public static final Quoter JSON = (value, outerQuotes) -> {
        String result = Strings.CS.replace(Objects.toString(value), "\"", "\\\"");
        result = Strings.CS.replace(result, "\n", "\\n");
        result = StringUtils.replaceChars(result, "\r\t", null);
        if (outerQuotes) {
            return '"' + result + '"';
        } else {
            return result;
        }
    };
    public static final Quoter URL = (value, outerQuotes) -> {
        if (outerQuotes) {
            return StringHelper.quoteForUrl(value);
        } else {
            return StringHelper.encodeForUrl(value);
        }
    };
    public static final Quoter NONE = (value, outerQuotes) -> Objects.toString(value);

    public static interface Quoter {

        public String quote(Object value, boolean outerQuotes);
    }
}
