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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Alert;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates tuples from a JDBC connection.
 */
public class TupleSourceJdbc implements TupleSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TupleSourceJdbc.class.getName());
    private static final String FAILED_TO_CLOSE_CONNECTION = "Exception trying to close connection.";
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

    private BasicDataSource ds;
    private Connection conn;

    public BasicDataSource getDataSource() {
        if (ds != null) {
            return ds;
        }
        try {
            LOGGER.info("  Loading driver {}", dbDriver);
            Class.forName(dbDriver);
        } catch (ClassNotFoundException ex) {
            LOGGER.error(FAILED_TO_LOAD_DB_DRIVER, ex);
            alertError(FAILED_TO_LOAD_DB_DRIVER, ex);
            return null;
        }
        ds = new BasicDataSource();
        ds.setUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        return ds;
    }

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                LOGGER.error(FAILED_TO_CLOSE_CONNECTION, ex);
                alertError(FAILED_TO_CLOSE_CONNECTION, ex);
            }
            conn = null;
        }
        if (ds != null) {
            try {
                ds.close();
            } catch (SQLException ex) {
                LOGGER.error(FAILED_TO_CLOSE_CONNECTION, ex);
                alertError(FAILED_TO_CLOSE_CONNECTION, ex);
            }
            ds = null;
        }
    }

    @Override
    public Iterator<Tuple> iterator() {
        BasicDataSource myDs = getDataSource();
        if (conn == null) {
            try {
                conn = myDs.getConnection();
                conn.isValid(1);
            } catch (SQLException ex) {
                LOGGER.error(FAILED_TO_CONNECT, ex);
                alertError(FAILED_TO_CONNECT, ex);
                return Collections.emptyIterator();
            }
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

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public void setDbQuery(String dbQuery) {
        this.dbQuery = dbQuery;
    }

    private void alertError(String text, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(text);
        alert.setContentText(ex.getLocalizedMessage());
        alert.showAndWait();
    }

    public static class JdbcTuple implements Tuple {

        private Map<String, Object> data = new HashMap<>();

        public JdbcTuple(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public Object get(String name) {
            return data.get(name);
        }

    }

    public static class JdbcTupleIterator implements Iterator<Tuple> {

        private final List<String> colNames = new ArrayList<>();
        private final ResultSet resultSet;
        private JdbcTuple current;
        private JdbcTuple next;

        public JdbcTupleIterator(ResultSet resultSet) {
            this.resultSet = resultSet;
            findColumnNames();
            fetchNext();
        }

        private void findColumnNames() {
            try {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int colCount = metaData.getColumnCount();
                for (int idx = 1; idx <= colCount; idx++) {
                    colNames.add(metaData.getColumnName(idx));
                }
            } catch (SQLException ex) {
                LOGGER.error("Failed to fetch column names.", ex);
            }
        }

        private Map<String, Object> extractData() throws SQLException {
            Map<String, Object> data = new HashMap<>();
            int idx = 1;
            for (var name : colNames) {
                data.put(name, resultSet.getObject(idx));
                idx++;
            }
            return data;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        private void fetchNext() {
            next = null;
            try {
                if (resultSet.next()) {
                    next = new JdbcTuple(extractData());
                }
            } catch (SQLException ex) {
                LOGGER.error("Failed to fetch next row.", ex);
            }
        }

        @Override
        public Tuple next() {
            current = next;
            next = null;
            fetchNext();
            return current;
        }

    }

}
