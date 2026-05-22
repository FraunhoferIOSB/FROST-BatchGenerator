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

import static net.time4j.PlainTime.HOUR_FROM_0_TO_24;
import static net.time4j.PlainTime.MINUTE_OF_HOUR;
import static net.time4j.PlainTime.NANO_OF_SECOND;
import static net.time4j.PlainTime.SECOND_OF_MINUTE;
import static net.time4j.format.expert.IsoDateStyle.EXTENDED_CALENDAR_DATE;
import static net.time4j.format.expert.IsoDecimalStyle.DOT;
import static net.time4j.tz.ZonalOffset.UTC;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.time4j.Moment;
import net.time4j.PlainDate;
import net.time4j.PlainTime;
import net.time4j.engine.ChronoCondition;
import net.time4j.engine.ChronoDisplay;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.format.Attributes;
import net.time4j.format.Leniency;
import net.time4j.format.NumberSystem;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.format.expert.ChronoPrinter;
import net.time4j.format.expert.Iso8601Format;
import net.time4j.format.expert.IsoDateStyle;
import net.time4j.format.expert.IsoDecimalStyle;
import net.time4j.range.MomentInterval;
import net.time4j.tz.ZonalOffset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various String helper methods.
 */
public class StringHelper {

    public static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final Logger LOGGER = LoggerFactory.getLogger(StringHelper.class);
    private static final String UTF8_NOT_SUPPORTED = "UTF-8 not supported?";
    private static final NonZeroCondition NON_ZERO_FRACTION = new NonZeroCondition(PlainTime.NANO_OF_SECOND);

    public static final ChronoPrinter<Moment> FORMAT_MOMENT = buildMomentFormatter();
    public static final ChronoPrinter<MomentInterval> FORMAT_INTERVAL = buildIntervalFormatter();

    private StringHelper() {
        // Utility class, not to be instantiated.
    }

    private static <T extends ChronoEntity<T>> void addWallTime(ChronoFormatter.Builder<T> builder, boolean extended, IsoDecimalStyle decimalStyle) {

        builder.startSection(Attributes.NUMBER_SYSTEM, NumberSystem.ARABIC);
        builder.startSection(Attributes.ZERO_DIGIT, '0');
        builder.addFixedInteger(HOUR_FROM_0_TO_24, 2);

        if (extended) {
            builder.addLiteral(':');
        }

        builder.addFixedInteger(MINUTE_OF_HOUR, 2);

        if (extended) {
            builder.addLiteral(':');
        }

        builder.addFixedInteger(SECOND_OF_MINUTE, 2);
        builder.startOptionalSection(NON_ZERO_FRACTION);

        switch (decimalStyle) {
            case COMMA:
                builder.addLiteral(',', '.');
                break;
            case DOT:
                builder.addLiteral('.', ',');
                break;
            default:
                throw new UnsupportedOperationException(decimalStyle.name());
        }

        builder.addFraction(NANO_OF_SECOND, 1, 9, false);

        for (int i = 0; i < 3; i++) {
            builder.endSection();
        }

    }

    public static String afterLastSlash(final String input) {
        return input.substring(input.lastIndexOf('/') + 1);
    }

    private static ChronoPrinter<MomentInterval> buildIntervalFormatter() {
        return (formattable, buffer, attributes) -> {
            MomentInterval interval = formattable.toCanonical();
            if (interval.getStart().isInfinite()) {
                buffer.append("-");
            } else {
                FORMAT_MOMENT.print(interval.getStartAsMoment(), buffer);
            }
            buffer.append('/');
            if (interval.getEnd().isInfinite()) {
                buffer.append("-");
            } else {
                FORMAT_MOMENT.print(interval.getEndAsMoment(), buffer);
            }
            return Collections.emptySet();
        };
    }

    private static ChronoPrinter<Moment> buildMomentFormatter() {
        IsoDateStyle dateStyle = EXTENDED_CALENDAR_DATE;
        IsoDecimalStyle decimalStyle = DOT;
        ZonalOffset offset = UTC;
        ChronoFormatter.Builder<Moment> builder = ChronoFormatter.setUp(Moment.axis(), Locale.ROOT);
        builder.addCustomized(
                PlainDate.COMPONENT,
                Iso8601Format.ofDate(dateStyle),
                (text, status, attributes) -> null);
        builder.addLiteral('T');
        addWallTime(builder, dateStyle.isExtended(), decimalStyle);
        builder.addTimezoneOffset(FormatStyle.MEDIUM, dateStyle.isExtended(), Collections.singletonList("Z"));
        return builder.build().with(Leniency.STRICT).withTimezone(offset);

    }

    public static String camelCase(final String name) {
        final String[] parts = StringUtils.split(name, '_');
        final StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        for (int idx = 1; idx < parts.length; idx++) {
            final String part = parts[idx];
            result.append(part.substring(0, 1).toUpperCase());
            result.append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * Replaces characters that might break logging output. Currently: \n, \r
     * and \t
     *
     * @param string The string to clean.
     * @return The cleaned string.
     */
    public static String cleanForLogging(String string) {
        if (string == null) {
            return "null";
        }
        return StringUtils.replaceChars(string, "\n\r\t", "");
    }

    /**
     * Null-Save replaces characters that might break logging output. Currently:
     * \n, \r and \t
     *
     * @param object The Object to clean.
     * @return The cleaned string.
     */
    public static String cleanForLogging(Object object) {
        return cleanForLogging(Objects.toString(object));
    }

    /**
     * Removes characters that might break logging output and truncates to a
     * maximum length.Currently: \n, \r and \t
     *
     * @param string The string to clean.
     * @param maxLength The maximum length of the returned String.
     * @return The cleaned string.
     */
    public static String cleanForLogging(String string, int maxLength) {
        return StringUtils.replaceChars(StringUtils.abbreviate(string, maxLength), "\n\r\t", "");
    }

    /**
     * Changes the first letter of the string into lower case.
     *
     * @param string The string to de-capitalise.
     * @return The de-capitalised string.
     */
    public static String deCapitalize(String string) {
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    /**
     * Replaces all ' in the string with ''.
     *
     * @param in The string to escape.
     * @return The escaped string.
     */
    public static String escapeForStringConstant(String in) {
        return in.replace("'", "''");
    }

    /**
     * Returns true if the given string is null, or empty.
     *
     * @param string the string to check.
     * @return true if string == null || string.isEmpty()
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isNullOrEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isNullOrEmpty(Collection arr) {
        return arr == null || arr.isEmpty();
    }

    public static boolean isNullOrEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    /**
     * Quote the given value for use in URLs.
     *
     * @param in The object to quote.
     * @return The quoted String.
     */
    public static String quoteForUrl(Object in) {
        if (in instanceof Number) {
            return in.toString();
        }
        if (in == null) {
            return "null";
        }
        return "'" + escapeForStringConstant(in.toString()) + "'";
    }

    /**
     * Decode the given input using UTF-8 as character set.
     *
     * @param input The input to urlDecode.
     * @return The decoded input.
     */
    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, UTF8.name());
        } catch (UnsupportedEncodingException exc) {
            // Should never happen, UTF-8 is build in.
            LOGGER.error(UTF8_NOT_SUPPORTED);
            throw new IllegalStateException(UTF8_NOT_SUPPORTED, exc);
        }
    }

    /**
     * Urlencodes the given string, optionally not encoding forward slashes.
     *
     * In urls, forward slashes before the "?" must never be urlEncoded.
     * Urlencoding of slashes could otherwise be used to obfuscate phising URLs.
     *
     * @param string The string to urlEncode.
     * @param notSlashes If true, forward slashes are not encoded.
     * @return The urlEncoded string.
     */
    public static String urlEncode(String string, boolean notSlashes) {
        if (notSlashes) {
            return urlEncodeNotSlashes(string);
        }
        return urlEncode(string);
    }

    public static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, UTF8.name());
        } catch (UnsupportedEncodingException exc) {
            // Should never happen, UTF-8 is build in.
            LOGGER.error(UTF8_NOT_SUPPORTED);
            throw new IllegalStateException(UTF8_NOT_SUPPORTED, exc);
        }
    }

    /**
     * Urlencodes the given string, except for the forward slashes.
     *
     * @param string The string to urlEncode.
     * @return The urlEncoded string.
     */
    public static String urlEncodeNotSlashes(String string) {
        String[] split = string.split("/");
        for (int i = 0; i < split.length; i++) {
            split[i] = urlEncode(split[i]);
        }
        return String.join("/", split);
    }

    private static class NonZeroCondition implements ChronoCondition<ChronoDisplay> {

        private final ChronoElement<Integer> element;

        NonZeroCondition(ChronoElement<Integer> element) {
            this.element = element;
        }

        @Override
        public boolean test(ChronoDisplay context) {
            return (context.getInt(this.element) > 0);
        }

        ChronoCondition<ChronoDisplay> or(final NonZeroCondition other) {
            return context -> (NonZeroCondition.this.test(context) || other.test(context));
        }
    }

}
