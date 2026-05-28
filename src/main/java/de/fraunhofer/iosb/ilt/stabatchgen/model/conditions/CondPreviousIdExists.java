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
package de.fraunhofer.iosb.ilt.stabatchgen.model.conditions;

import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorBoolean;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.stabatchgen.model.source.Tuple;
import de.fraunhofer.iosb.ilt.stabatchgen.utils.TemplateUtils;
import java.util.Set;

/**
 * A condition that returns true if a given Id was registered previously in the
 * same batch. Unless negate is set, then return the opposite.
 */
public class CondPreviousIdExists implements Condition {

    @ConfigurableField(editor = EditorString.class,
            label = "ID Template", description = "The template for the batch item ID.")
    @EditorString.EdOptsString(dflt = "item-{idColumn}")
    private String templateId;

    @ConfigurableField(editor = EditorBoolean.class,
            label = "Negate", description = "Return true when the ID is NOT set.")
    @EditorBoolean.EdOptsBool(dflt = true)
    private boolean negate;

    @Override
    public boolean resolveFor(Tuple tuple, Set<String> previousIds) {
        final String idValue = TemplateUtils.fillPlainTemplate(templateId, tuple);
        if (negate) {
            return !previousIds.contains(idValue);
        }
        return previousIds.contains(idValue);
    }
}
