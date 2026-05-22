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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * A single request from a JSON Batch request.
 */
@JsonPropertyOrder({"if", "id", "atomicityGroup", "method", "url", "headers", "body"})
public class JsonBatchRequestItem {

    @JsonProperty(value = "if")
    private String ifCondition;
    private String id;
    private String atomicityGroup;
    private String method;
    private String url;
    private Map<String, String> headers;
    private JsonNode body;

    public String getAtomicityGroup() {
        return atomicityGroup;
    }

    public JsonBatchRequestItem setAtomicityGroup(String atomicityGroup) {
        this.atomicityGroup = atomicityGroup;
        return this;
    }

    public JsonNode getBody() {
        return body;
    }

    public JsonBatchRequestItem setBody(JsonNode body) {
        this.body = body;
        return this;
    }

    public Map<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<>();
        }
        return headers;
    }

    public void addHeader(String name, String value) {
        getHeaders().put(name, value);
    }

    public JsonBatchRequestItem setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public String getId() {
        return id;
    }

    public JsonBatchRequestItem setId(String id) {
        this.id = id;
        return this;
    }

    public String getIf() {
        return ifCondition;
    }

    public void setIf(String ifCondition) {
        this.ifCondition = ifCondition;
    }

    public String getMethod() {
        return method;
    }

    public JsonBatchRequestItem setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public JsonBatchRequestItem setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public String toString() {
        return id;
    }

}
