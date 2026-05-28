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
import de.fraunhofer.iosb.ilt.configurable.editor.EditorList;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorSubclass;
import de.fraunhofer.iosb.ilt.stabatchgen.model.source.Tuple;
import java.util.List;
import java.util.Set;

/**
 * A condition that is true when any sub-conditions is true. Evaluated as
 * succeed-fast.
 * F
 */
public class ConditionOr implements Condition {

    @ConfigurableField(editor = EditorList.class, optional = false,
            label = "Conditions", description = "The list of conditions to check, fail-fast.")
    @EditorList.EdOptsList(editor = EditorSubclass.class)
    @EditorSubclass.EdOptsSubclass(iface = Condition.class, shortenClassNames = true)
    private List<Condition> conditions;

    @Override
    public boolean resolveFor(Tuple tuple, Set<String> previousIds) {
        for (var condition : conditions) {
            if (condition.resolveFor(tuple, previousIds)) {
                return true;
            }
        }
        return false;
    }

}
