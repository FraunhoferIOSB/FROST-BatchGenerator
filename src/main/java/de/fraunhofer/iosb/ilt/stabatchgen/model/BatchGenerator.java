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
import de.fraunhofer.iosb.ilt.configurable.editor.EditorBoolean;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorClass;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorInt;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorList;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorSubclass;
import de.fraunhofer.iosb.ilt.stabatchgen.model.source.Tuple;
import de.fraunhofer.iosb.ilt.stabatchgen.model.source.TupleSource;
import de.fraunhofer.iosb.ilt.stabatchgen.utils.SimpleJsonMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Main model class for batch file generators.
 */
public class BatchGenerator implements AnnotatedConfigurable<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchGenerator.class.getName());

    @ConfigurableField(editor = EditorBoolean.class,
            label = "Pretty", description = "Should the content of the file be prettified.")
    @EditorBoolean.EdOptsBool(dflt = true)
    private boolean prettyPrint;

    @ConfigurableField(editor = EditorBoolean.class,
            label = "Compress", description = "Use GZIP compression, automatically adds .gz to the file name.")
    @EditorBoolean.EdOptsBool(dflt = false)
    private boolean compress;

    @ConfigurableField(editor = EditorString.class,
            label = "FileName", description = "The filename to write the batch request to.")
    @EditorString.EdOptsString(dflt = "batch-%03d.json")
    private String fileNameTemplate;

    @ConfigurableField(editor = EditorInt.class,
            label = "Per File", description = "The maximum number of items per batch file.")
    @EditorInt.EdOptsInt(dflt = 1000, min = 0, max = 1_000_000)
    private int itemsPerFile;

    @ConfigurableField(editor = EditorSubclass.class,
            label = "Source", description = "The source for rows to convert")
    @EditorSubclass.EdOptsSubclass(iface = TupleSource.class, shortenClassNames = true)
    private TupleSource source;

    @ConfigurableField(editor = EditorList.class,
            label = "Items", description = "The templates for the Batch Items to generate from each tuple.")
    @EditorList.EdOptsList(editor = EditorClass.class, minCount = 1)
    @EditorClass.EdOptsClass(clazz = BatchItemGenerator.class)
    private List<BatchItemGenerator> items;

    public void execute(Path directory) throws IOException {
        LOGGER.info("Starting work on {}", fileNameTemplate);
        int batchIdx = 1;
        int inBatch = 0;
        JsonBatchRequest currentBatch = null;
        try {
            currentBatch = createBatch(directory, batchIdx);
            Set<String> previousIds = new HashSet<>();

            for (Tuple tuple : source) {
                for (BatchItemGenerator item : items) {
                    List<JsonBatchRequestItem> results = item.applyTo(tuple, previousIds);
                    for (JsonBatchRequestItem result : results) {
                        final String batchId = result.getId();
                        previousIds.add(batchId);
                        currentBatch.addRequest(result);
                        LOGGER.trace("    Added {} ", batchId);
                    }
                    inBatch += results.size();
                }
                LOGGER.trace("    Added {} of {}", inBatch, itemsPerFile);

                if (inBatch > itemsPerFile) {
                    LOGGER.info("    Added {} of {}, next file.", inBatch, itemsPerFile);
                    closeBatch(currentBatch);
                    batchIdx++;
                    previousIds = new HashSet<>();
                    currentBatch = createBatch(directory, batchIdx);
                    inBatch = 0;
                }
            }
        } finally {
            if (currentBatch != null) {
                closeBatch(currentBatch);
            }
        }
        LOGGER.info("Finished {}", fileNameTemplate);
    }

    public void closeBatch(JsonBatchRequest batch) {
        if (batch != null) {
            batch.close();
        }
    }

    public JsonBatchRequest createBatch(Path directory, int batchIdx) throws IOException {
        String fileName = String.format(fileNameTemplate, batchIdx);
        if (compress) {
            fileName += ".gz";
        }

        File file = directory.resolve(fileName).toFile();
        LOGGER.info("  Opening file {} for writing", file);
        if (!file.exists()) {
            file.createNewFile();
        }
        JsonBatchRequest jsonBatchRequest = new JsonBatchRequest();
        writeInBackground(file, jsonBatchRequest);
        return jsonBatchRequest;
    }

    public void setFileNameTemplate(String fileNameTemplate) {
        this.fileNameTemplate = fileNameTemplate;
    }

    public void setItemsPerFile(int itemsPerFile) {
        this.itemsPerFile = itemsPerFile;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public void setSource(TupleSource source) {
        this.source = source;
    }

    public void setItems(List<BatchItemGenerator> items) {
        this.items = items;
    }

    public void writeInBackground(File file, Object value) {
        Thread t = new Thread(
                () -> {

                    final ObjectMapper mapper;
                    if (prettyPrint) {
                        mapper = SimpleJsonMapper.getPrettyMapper();
                    } else {
                        mapper = SimpleJsonMapper.getObjectMapper();
                    }
                    if (compress) {
                        GZIPOutputStream os;
                        try {
                            os = new GZIPOutputStream(new FileOutputStream(file));
                            mapper.writeValue(os, value);
                        } catch (IOException ex) {
                            LOGGER.error("Failed to write file {}", file, ex);
                        }
                    } else {
                        mapper.writeValue(file, value);
                    }
                    LOGGER.info("  Finished writing {}", file);
                },
                file.getName());
        t.start();
    }

}
