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
package de.fraunhofer.iosb.ilt.stabatchgen.model;

import de.fraunhofer.iosb.ilt.configurable.AnnotatedConfigurable;
import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorClass;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorList;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.stabatchgen.model.source.Tuple;
import de.fraunhofer.iosb.ilt.stabatchgen.utils.SimpleJsonMapper;
import de.fraunhofer.iosb.ilt.stabatchgen.utils.StringHelper;
import de.fraunhofer.iosb.ilt.stabatchgen.utils.TemplateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates a single batch request item.
 */
public class BatchItemGenerator implements AnnotatedConfigurable<Object, Object> {

    private static final String BODY_TEMLATE_DEFAULT = """
            {
              "name": "Station",
              "description": "A sensor station"
            }""";

    @ConfigurableField(editor = EditorString.class,
            label = "Group", description = "The group the batch item is in.")
    @EditorString.EdOptsString(dflt = "group1")
    private String group;

    @ConfigurableField(editor = EditorString.class,
            label = "ID Template", description = "The template for the batch item ID.")
    @EditorString.EdOptsString(dflt = "item-{idColumn}")
    private String templateId;

    @ConfigurableField(editor = EditorString.class,
            label = "Method", description = "The HTTP method the batch item uses.")
    @EditorString.EdOptsString(dflt = "POST")
    private String method;

    @ConfigurableField(editor = EditorString.class,
            label = "URL Template", description = "The template for the URL the batch item uses.")
    @EditorString.EdOptsString(dflt = "Things")
    private String templateUrl;

    @ConfigurableField(editor = EditorList.class, optional = true,
            label = "Header Templates", description = "The templates for the Headers.")
    @EditorList.EdOptsList(editor = EditorClass.class)
    @EditorClass.EdOptsClass(clazz = Header.class)
    private List<Header> headers;

    @ConfigurableField(editor = EditorString.class,
            label = "IF Template", description = "The template for the IF the batch item uses.")
    @EditorString.EdOptsString(dflt = "")
    private String templateIf;

    @ConfigurableField(editor = EditorString.class,
            label = "Body Template", description = "The template for the batch item body.")
    @EditorString.EdOptsString(dflt = BODY_TEMLATE_DEFAULT, lines = 10)
    private String templateBody;

    public List<JsonBatchRequestItem> applyTo(Tuple tuple, Set<String> previousIds) {
        List<JsonBatchRequestItem> result = new ArrayList<>();
        JsonBatchRequestItem item = new JsonBatchRequestItem()
                .setAtomicityGroup(group)
                .setId(TemplateUtils.fillPlainTemplate(templateId, tuple))
                .setMethod(method)
                .setUrl(TemplateUtils.fillUrlTemplate(templateUrl, tuple));

        final String bodyString = TemplateUtils.fillJsonTemplate(templateBody, tuple);
        if (!StringHelper.isNullOrEmpty(bodyString.trim())) {
            item.setBody(SimpleJsonMapper.jsonToTree(bodyString));
        }

        if (!StringHelper.isNullOrEmpty(templateIf)) {
            item.setIf(TemplateUtils.fillPlainTemplate(templateIf, tuple));
        }

        if (!StringHelper.isNullOrEmpty(headers)) {
            for (var header : headers) {
                item.addHeader(header.getName(), header.getValue());
            }
        }

        result.add(item);
        return result;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public String getTemplateIf() {
        return templateIf;
    }

    public void setTemplateIf(String templateIf) {
        this.templateIf = templateIf;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
    }

    public static class Header implements AnnotatedConfigurable<Object, Object> {

        @ConfigurableField(editor = EditorString.class,
                label = "Name", description = "The name of the header.")
        @EditorString.EdOptsString(dflt = "Content-Type")
        private String name;

        @ConfigurableField(editor = EditorString.class,
                label = "Value", description = "The value of the header")
        @EditorString.EdOptsString(dflt = "application/json")
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }
}
