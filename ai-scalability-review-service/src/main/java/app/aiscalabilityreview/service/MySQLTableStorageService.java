package app.aiscalabilityreview.service;

import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import core.framework.api.json.Property;
import core.framework.json.JSON;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects the top-20 MySQL tables by storage size for the local review, over JDBC, and writes them
 * to {@code mysql-tables.md} for the metric collection stage to copy into its output.
 *
 * <p>This does not go through the MySQL MCP server, because it cannot: Gemini CLI 0.46 wraps every
 * MCP response in {@code fixStructuredContent()}, which JSON-parses the first text block and assigns
 * it to {@code result.structuredContent}, and the MCP SDK then validates that field against
 * {@code record(string, unknown)}. {@code @benborla29/mcp-server-mysql} serialises result rows as a
 * JSON <em>array</em>, so every row-returning query fails client-side with
 * {@code Invalid input: expected record, received array} — which is exactly the
 * "MySQL MCP error — Invalid response format from query tool" seen in earlier reports. The query is
 * fixed and read-only, so running it here costs nothing and cannot fail that way.
 *
 * <p>Connection details come from the environment when set, otherwise from the MCP server entry in
 * {@code ~/.gemini/settings.json} keyed by {@link LocalReviewPromptBuilder#mysqlConnectionName} —
 * that entry is already required for a local run, and it carries the routable host, which the
 * Datadog/Azure resource name in the request does not.
 */
public class MySQLTableStorageService {
    private static final String TOP_TABLES_SQL = """
            SELECT
                table_name,
                table_rows,
                ROUND(data_length / 1024 / 1024, 2)   AS data_mb,
                ROUND(index_length / 1024 / 1024, 2)  AS index_mb,
                ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_mb
            FROM information_schema.tables
            WHERE table_schema = ?
            ORDER BY (data_length + index_length) DESC
            LIMIT 20
            """;
    private static final String SQL_ONE_LINE =
        "SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, "
            + "ROUND(index_length/1024/1024,2) AS index_mb, "
            + "ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables "
            + "WHERE table_schema='{}' ORDER BY (data_length+index_length) DESC LIMIT 20";
    private static final String SUMMARY_TEMPLATE = """

        - **Largest table**: `{}` at {} GB ({} rows)
        - **Tables above 10 GB**: {}
        - **Total of the top {} tables**: {} GB

        {}
        """;
    private static final double MB_PER_GB = 1024;
    private static final double TEN_GB_IN_MB = 10 * MB_PER_GB;
    private static final int QUERY_TIMEOUT_SECONDS = 30;

    private final Logger logger = LoggerFactory.getLogger(MySQLTableStorageService.class);

    /**
     * Writes the table-storage section and returns a one-line summary for the stage status. Never
     * throws: a service with no MySQL, an unreachable database or a missing credential all produce a
     * {@code NOT_COLLECTED:} line in the output file, which is what the later stages expect to read.
     */
    public String collect(MySQLTableParams params) {
        String body = render(params);
        Path path = Path.of(params.outputPath()).toAbsolutePath();
        try {
            Files.writeString(path, body);
        } catch (IOException e) {
            logger.warn("Could not write MySQL table storage to {}: {}", path, e.getMessage());
            return "MySQL table storage not written: " + e.getMessage();
        }
        return body.startsWith("NOT_COLLECTED") ? body.lines().findFirst().orElse("NOT_COLLECTED") : "MySQL table storage collected";
    }

    private String render(MySQLTableParams params) {
        if (isAbsent(params.mysqlHost()) || isAbsent(params.mysqlDb())) {
            return "NOT_COLLECTED: MySQL not configured for this service (mysql_host or mysql_db is empty)\n";
        }
        ConnectionSettings connection = connectionSettings(params);
        if (connection == null) {
            return Strings.format("NOT_COLLECTED: MySQL error — no credentials for `{}`. Set MYSQL_USER and MYSQL_PASSWORD, "
                + "or add an entry named `{}` to ~/.gemini/settings.json.\n",
                params.mysqlDb(), LocalReviewPromptBuilder.mysqlConnectionName(params.mysqlHost(), params.mysqlDb()));
        }
        try {
            List<TableRow> rows = query(connection, params.mysqlDb());
            logger.info("Collected {} MySQL tables from {}/{}", rows.size(), connection.host(), params.mysqlDb());
            return table(rows, params.mysqlHost(), params.mysqlDb());
        } catch (SQLException e) {
            logger.warn("MySQL table storage query failed for {}/{}: {}", connection.host(), params.mysqlDb(), e.getMessage());
            return Strings.format("NOT_COLLECTED: MySQL error — {}\n", e.getMessage());
        }
    }

    private List<TableRow> query(ConnectionSettings settings, String database) throws SQLException {
        String url = Strings.format("jdbc:mysql://{}:{}/{}?useSSL=true&requireSSL=false&connectTimeout=10000&socketTimeout=30000",
            settings.host(), settings.port(), database);
        try (Connection conn = DriverManager.getConnection(url, settings.user(), settings.password());
             PreparedStatement statement = conn.prepareStatement(TOP_TABLES_SQL)) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setString(1, database);
            return read(statement);
        }
    }

    private List<TableRow> read(PreparedStatement statement) throws SQLException {
        List<TableRow> rows = new ArrayList<>(20);
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(new TableRow(resultSet.getString("table_name"), resultSet.getLong("table_rows"),
                    resultSet.getDouble("data_mb"), resultSet.getDouble("index_mb"), resultSet.getDouble("total_mb")));
            }
        }
        return rows;
    }

    private String table(List<TableRow> rows, String host, String database) {
        if (rows.isEmpty()) {
            return Strings.format("NOT_COLLECTED: MySQL returned no rows for schema `{}` — the database is empty "
                + "or the account cannot read information_schema.tables\n", database);
        }
        String source = Strings.format("[Source: MySQL ({}/{}) → {}]", host, database, Strings.format(SQL_ONE_LINE, database));
        StringBuilder markdown = new StringBuilder(2048);
        markdown.append("| Table | Rows | Data (MB) | Index (MB) | Total (MB) |\n| :--- | ---: | ---: | ---: | ---: |\n");
        double totalMb = 0;
        int over10Gb = 0;
        for (TableRow row : rows) {
            markdown.append(Strings.format("| {} | {} | {} | {} | {} |\n", row.name(), row.rows(),
                twoDecimals(row.dataMb()), twoDecimals(row.indexMb()), twoDecimals(row.totalMb())));
            totalMb += row.totalMb();
            if (row.totalMb() > TEN_GB_IN_MB) over10Gb++;
        }
        TableRow largest = rows.getFirst();
        return markdown.append(Strings.format(SUMMARY_TEMPLATE,
            largest.name(), twoDecimals(largest.totalMb() / MB_PER_GB), largest.rows(),
            over10Gb, rows.size(), twoDecimals(totalMb / MB_PER_GB), source)).toString();
    }

    /**
     * Environment wins, so a run can point at a different account without editing the Gemini
     * settings; the host falls back to the settings entry because the request carries the Azure
     * resource name, which is not necessarily resolvable from this machine.
     */
    private ConnectionSettings connectionSettings(MySQLTableParams params) {
        Map<String, String> configured = geminiMcpEnv(LocalReviewPromptBuilder.mysqlConnectionName(params.mysqlHost(), params.mysqlDb()));
        String user = firstSet(System.getenv("MYSQL_USER"), configured.get("MYSQL_USER"));
        String password = firstSet(System.getenv("MYSQL_PASSWORD"), configured.get("MYSQL_PASS"));
        if (user == null || password == null) return null;
        String host = firstSet(System.getenv("MYSQL_JDBC_HOST"), configured.get("MYSQL_HOST"), params.mysqlHost());
        String port = firstSet(System.getenv("MYSQL_PORT"), configured.get("MYSQL_PORT"), "3306");
        return new ConnectionSettings(host, port, user, password);
    }

    private Map<String, String> geminiMcpEnv(String name) {
        Path settingsPath = Path.of(System.getProperty("user.home"), ".gemini", "settings.json");
        if (name == null || !Files.exists(settingsPath)) return Map.of();
        try {
            GeminiSettings settings = JSON.fromJSON(GeminiSettings.class, Files.readString(settingsPath));
            if (settings.mcpServers == null) return Map.of();
            GeminiMCPServer server = settings.mcpServers.get(name);
            return server == null || server.env == null ? Map.of() : server.env;
        } catch (IOException | RuntimeException e) {
            logger.warn("Could not read MySQL connection settings for {} from {}: {}", name, settingsPath, e.getMessage());
            return Map.of();
        }
    }

    private String firstSet(String... values) {
        for (String value : values) {
            if (!Strings.isBlank(value)) return value;
        }
        return null;
    }

    private String twoDecimals(double value) {
        return String.format("%.2f", value);
    }

    private boolean isAbsent(String value) {
        return Strings.isBlank(value) || "N/A".equalsIgnoreCase(value);
    }

    public record MySQLTableParams(String mysqlHost, String mysqlDb, String outputPath) {
    }

    private record ConnectionSettings(String host, String port, String user, String password) {
    }

    private record TableRow(String name, long rows, double dataMb, double indexMb, double totalMb) {
    }

    public static class GeminiSettings {
        @Property(name = "mcpServers")
        public Map<String, GeminiMCPServer> mcpServers;
    }

    public static class GeminiMCPServer {
        @Property(name = "env")
        public Map<String, String> env;
    }
}
