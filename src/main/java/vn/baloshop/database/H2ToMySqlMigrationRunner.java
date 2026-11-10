package vn.baloshop.database;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "shop.database.migrate-from-h2", havingValue = "true")
public class H2ToMySqlMigrationRunner implements ApplicationRunner {
    private static final List<String> TABLES =
            List.of("customer_accounts", "products", "shop_orders", "order_items");

    private final DataSource targetDataSource;
    private final Environment environment;

    public H2ToMySqlMigrationRunner(DataSource targetDataSource, Environment environment) {
        this.targetDataSource = targetDataSource;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String sourceUrl = environment.getRequiredProperty("shop.database.migration.source-url");
        String sourceUser = environment.getRequiredProperty("shop.database.migration.source-username");
        String sourcePassword = environment.getProperty("shop.database.migration.source-password", "");

        try (Connection source = java.sql.DriverManager.getConnection(sourceUrl, sourceUser, sourcePassword);
             Connection target = targetDataSource.getConnection()) {
            if (!target.getMetaData().getURL().startsWith("jdbc:mysql:")) {
                throw new IllegalStateException("H2 data migration requires MySQL as the configured target database.");
            }
            verifyTargetIsEmpty(target);
            target.setAutoCommit(false);
            try {
                setForeignKeyChecks(target, false);
                for (String table : TABLES) {
                    int copied = copyTable(source, target, table);
                    System.out.printf("Migrated %d rows from %s.%n", copied, table);
                }
                target.commit();
            } catch (Exception exception) {
                target.rollback();
                throw exception;
            } finally {
                setForeignKeyChecks(target, true);
                target.setAutoCommit(true);
            }
        }
    }

    private void verifyTargetIsEmpty(Connection target) throws SQLException {
        for (String table : TABLES) {
            String actualTable = findTable(target, table);
            if (actualTable == null) {
                throw new IllegalStateException("MySQL table is missing: " + table);
            }
            try (Statement statement = target.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + quoteMySql(actualTable))) {
                rows.next();
                if (rows.getLong(1) != 0) {
                    throw new IllegalStateException("MySQL table is not empty: " + table
                            + ". Migration stopped without changing target data.");
                }
            }
        }
    }

    private int copyTable(Connection source, Connection target, String table) throws SQLException {
        String sourceTable = findTable(source, table);
        String targetTable = findTable(target, table);
        if (sourceTable == null || targetTable == null) {
            throw new IllegalStateException("Required migration table is missing: " + table);
        }

        List<String> targetColumns = getColumns(target, targetTable);
        List<String> sourceColumns = getColumns(source, sourceTable);
        List<String> columns = sourceColumns.stream()
                .filter(sourceColumn -> targetColumns.stream()
                        .anyMatch(targetColumn -> targetColumn.equalsIgnoreCase(sourceColumn)))
                .toList();
        if (columns.isEmpty()) {
            throw new IllegalStateException("No matching columns found for migration table: " + table);
        }

        String sourceSql = "SELECT * FROM " + quote(source, sourceTable);
        String targetSql = "INSERT INTO " + quoteMySql(targetTable) + " ("
                + String.join(", ", columns.stream().map(H2ToMySqlMigrationRunner::quoteMySql).toList())
                + ") VALUES (" + String.join(", ", columns.stream().map(column -> "?").toList()) + ")";
        int copied = 0;
        try (Statement select = source.createStatement();
             ResultSet rows = select.executeQuery(sourceSql);
             var insert = target.prepareStatement(targetSql)) {
            ResultSetMetaData metadata = rows.getMetaData();
            Map<String, Integer> sourceColumnIndexes = new LinkedHashMap<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                sourceColumnIndexes.put(metadata.getColumnLabel(index).toLowerCase(Locale.ROOT), index);
            }
            while (rows.next()) {
                for (int index = 0; index < columns.size(); index++) {
                    Object value = rows.getObject(sourceColumnIndexes.get(columns.get(index).toLowerCase(Locale.ROOT)));
                    insert.setObject(index + 1, normalizeValue(value));
                }
                insert.addBatch();
                copied++;
            }
            insert.executeBatch();
        }
        return copied;
    }

    private List<String> getColumns(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet metadata = connection.getMetaData().getColumns(connection.getCatalog(), null, table, null)) {
            while (metadata.next()) {
                columns.add(metadata.getString("COLUMN_NAME"));
            }
        }
        if (columns.isEmpty()) {
            try (ResultSet metadata = connection.getMetaData().getColumns(connection.getCatalog(), null,
                    table.toUpperCase(Locale.ROOT), null)) {
                while (metadata.next()) {
                    columns.add(metadata.getString("COLUMN_NAME"));
                }
            }
        }
        return columns;
    }

    private String findTable(Connection connection, String expectedName) throws SQLException {
        try (ResultSet metadata = connection.getMetaData().getTables(connection.getCatalog(), null, "%",
                new String[]{"TABLE"})) {
            while (metadata.next()) {
                String name = metadata.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private String quote(Connection connection, String identifier) throws SQLException {
        String quote = connection.getMetaData().getIdentifierQuoteString().trim();
        return quote.isEmpty() ? identifier : quote + identifier + quote;
    }

    private static String quoteMySql(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private void setForeignKeyChecks(Connection target, boolean enabled) throws SQLException {
        try (Statement statement = target.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=" + (enabled ? "1" : "0"));
        }
    }

    private Object normalizeValue(Object value) {
        if (value instanceof OffsetDateTime dateTime) {
            return Timestamp.from(dateTime.toInstant());
        }
        if (value instanceof ZonedDateTime dateTime) {
            return Timestamp.from(dateTime.toInstant());
        }
        return value;
    }
}
