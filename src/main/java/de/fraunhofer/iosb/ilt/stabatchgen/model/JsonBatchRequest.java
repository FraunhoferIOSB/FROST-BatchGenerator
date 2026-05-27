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
import de.fraunhofer.iosb.ilt.stabatchgen.utils.ClosableBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JSON Batch request, with a Blocking iterator holding the items.
 */
public class JsonBatchRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonBatchRequest.class.getName());

    private final ClosableBlockingQueue<JsonBatchRequestItem> requests;

    public JsonBatchRequest() {
        requests = new ClosableBlockingQueue<>(10);
    }

    public void close() {
        try {
            requests.close();
        } catch (InterruptedException ex) {
            LOGGER.error("Interrupted while trying to close request items.", ex);
        }
    }

    public void addRequest(JsonBatchRequestItem item) {
        try {
            requests.put(item);
        } catch (InterruptedException ex) {
            LOGGER.error("Failed to put item", ex);
        }
    }

    @JsonProperty
    public Iterable<JsonBatchRequestItem> getRequests() {
        return requests;
    }

}
