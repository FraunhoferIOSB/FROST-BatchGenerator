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
package de.fraunhofer.iosb.ilt.stabatchgen.model.source;

import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorPassword;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import javafx.scene.control.Alert;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates tuples from a JDBC connection.
 */
public class TupleSourceJdbc implements TupleSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TupleSourceJdbc.class.getName());
    private static final String FAILED_TO_CONNECT = "Failed to connect to database.";
    private static final String FAILED_TO_EXECUTE_QUERY = "Failed to execute query.";
    private static final String FAILED_TO_LOAD_DB_DRIVER = "Failed to load DB driver.";

    @ConfigurableField(editor = EditorString.class,
            label = "DB Driver", description = "The JDBC driver to use to connect to the database.")
    @EditorString.EdOptsString(dflt = "org.postgresql.Driver")
    private String dbDriver;

    @ConfigurableField(editor = EditorString.class,
            label = "DB URL", description = "The URL to use to connect to the database.")
    @EditorString.EdOptsString(dflt = "jdbc:postgresql://localhost:5432/sensorthings")
    private String dbUrl;

    @ConfigurableField(editor = EditorString.class,
            label = "DB Username", description = "The username to use to connect to the database.")
    @EditorString.EdOptsString(dflt = "")
    private String dbUsername;

    @ConfigurableField(editor = EditorPassword.class,
            label = "DB Password", description = "The password to use to connect to the database.")
    @EditorPassword.EdOptsPassword(dflt = "")
    private String dbPassword;

    @ConfigurableField(editor = EditorString.class,
            label = "Query", description = "The SQL Qeuery to use to fetch tuples.")
    @EditorString.EdOptsString(dflt = "SELECT * FROM table", lines = 5)
    private String dbQuery;

    public BasicDataSource getDataSource() {
        try {
            LOGGER.info("  Loading driver {}", dbDriver);
            Class.forName(dbDriver);
        } catch (ClassNotFoundException ex) {
            LOGGER.error(FAILED_TO_LOAD_DB_DRIVER, ex);
            alertError(FAILED_TO_LOAD_DB_DRIVER, ex);
            return null;
        }
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        return ds;
    }

    @Override
    public Iterator<Tuple> iterator() {
        BasicDataSource ds = getDataSource();
        Connection conn;
        try {
            conn = ds.getConnection();
            conn.isValid(1);
        } catch (SQLException ex) {
            LOGGER.error(FAILED_TO_CONNECT, ex);
            alertError(FAILED_TO_CONNECT, ex);
            return Collections.emptyIterator();
        }
        try {
            PreparedStatement stmnt = conn.prepareStatement(dbQuery);
            ResultSet resultSet = stmnt.executeQuery();
            return new JdbcTupleIterator(resultSet);
        } catch (SQLException ex) {
            LOGGER.error(FAILED_TO_EXECUTE_QUERY, ex);
            alertError(FAILED_TO_EXECUTE_QUERY, ex);
        }
        return Collections.emptyIterator();
    }

    private void alertError(String text, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(text);
        alert.setContentText(ex.getLocalizedMessage());
        alert.showAndWait();
    }

    public static class JdbcTuple implements Tuple {

        private final ResultSet resultSet;
        private boolean active;

        public JdbcTuple(ResultSet resultSet) {
            this.resultSet = resultSet;
            active = true;
        }

        public void deactivate() {
            active = false;
        }

        @Override
        public Object get(String name) {
            if (!active) {
                throw new IllegalStateException("Tuple is already deactivated.");
            }
            try {
                return resultSet.getObject(name);
            } catch (SQLException ex) {
                LOGGER.error("Failed to get {} from tuple {}", name, resultSet, ex);
                return null;
            }
        }

    }

    public static class JdbcTupleIterator implements Iterator<Tuple> {

        private final ResultSet resultSet;
        private JdbcTuple current;

        public JdbcTupleIterator(ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        @Override
        public boolean hasNext() {
            try {
                return !resultSet.isClosed() && !resultSet.isLast();
            } catch (SQLException ex) {
                LOGGER.error("Failed to fetch next row.", ex);
                return false;
            }
        }

        @Override
        public Tuple next() {
            if (current != null) {
                current.deactivate();
            }
            try {
                if (!resultSet.next()) {
                    LOGGER.error("Next returned false!");
                }
            } catch (SQLException ex) {
                LOGGER.error("Failed to fetch next row.", ex);
            }
            current = new JdbcTuple(resultSet);
            return current;
        }

    }
}
