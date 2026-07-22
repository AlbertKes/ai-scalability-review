package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.AuditLogService;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Stage 5: Query MySQL information_schema to get top-20 tables by storage size.
 *
 * Only runs when both mysqlHost and mysqlDb are configured.
 * Produces context.mysqlTableData with annotated query results.
 */
public class MySQLTableStage {
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
    private final Logger logger = LoggerFactory.getLogger(MySQLTableStage.class);
    @Inject
    AuditLogService auditLogService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;
        if (Strings.isBlank(config.runtime.mysqlHost) || Strings.isBlank(config.runtime.mysqlDB)) {
            context.mysqlTableData = "NOT_COLLECTED: MySQL MCP not configured (mysqlHost or mysqlDb is empty)";
            return;
        }
        String mysqlUser = System.getenv("MYSQL_USER");
        String mysqlPassword = System.getenv("MYSQL_PASSWORD");
        String mysqlPort = System.getenv("MYSQL_PORT");
        if (mysqlPort == null || mysqlPort.isBlank()) mysqlPort = "3306";
        if (mysqlUser == null || mysqlPassword == null) {
            context.mysqlTableData = "NOT_COLLECTED: MySQL MCP error — MYSQL_USER or MYSQL_PASSWORD not configured";
            return;
        }
        String jdbcUrl = "jdbc:mysql://" + config.runtime.mysqlHost + ':' + mysqlPort
                + '/' + config.runtime.mysqlDB + "?useSSL=true&requireSSL=false&connectTimeout=10000&socketTimeout=30000";
        logger.info("Querying MySQL table sizes for {}/{}", config.runtime.mysqlHost, config.runtime.mysqlDB);
        context.mysqlTableData = queryTableData(context, jdbcUrl, mysqlUser, mysqlPassword, config);
    }

    private String queryTableData(ReviewContext context, String jdbcUrl, String user, String password, ServiceConfig config) {
        long startMs = System.currentTimeMillis();
        StringBuilder tableData = buildTableHeader(config);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             PreparedStatement stmt = conn.prepareStatement(TOP_TABLES_SQL)) {
            stmt.setString(1, config.runtime.mysqlDB);
            ResultSet rs = stmt.executeQuery();
            int[] counts = appendTableRows(rs, tableData, config);
            int tableCount = counts[0];
            long durationMs = System.currentTimeMillis() - startMs;
            auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                    "MYSQL_TABLE_QUERY", "MySQLTableStage",
                    config.runtime.mysqlHost + "/" + config.runtime.mysqlDB,
                    "Top 20 tables by storage",
                    null, 200, durationMs,
                    "Fetched " + tableCount + " rows", null, true));
            logger.info("MySQL table query complete for {}/{}: {} tables", config.runtime.mysqlHost, config.runtime.mysqlDB, tableCount);
        } catch (SQLException e) {
            return handleException(context, e, startMs, config).toString();
        }
        return tableData.toString();
    }

    private StringBuilder buildTableHeader(ServiceConfig config) {
        return new StringBuilder(2048)
                .append("## MySQL Table Size Analysis\n\n```\n")
                .append("-- [Source: MySQL MCP (").append(config.runtime.mysqlHost).append('-').append(config.runtime.mysqlDB).append(") \u2192\n")
                .append("SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb,\n")
                .append("       ROUND(index_length/1024/1024,2) AS index_mb,\n")
                .append("       ROUND((data_length+index_length)/1024/1024,2) AS total_mb\n")
                .append("FROM information_schema.tables WHERE table_schema='").append(config.runtime.mysqlDB).append("'\n")
                .append("ORDER BY (data_length+index_length) DESC LIMIT 20;\n")
                .append("-- ]\n```\n\n")
                .append("| Table | Rows | Data (MB) | Index (MB) | Total (MB) |\n")
                .append("| :--- | ---: | ---: | ---: | ---: |\n");
    }

    private int[] appendTableRows(ResultSet rs, StringBuilder tableData, ServiceConfig config) throws SQLException {
        double totalMb = 0;
        int tableCount = 0;
        while (rs.next()) {
            String tableName = rs.getString("table_name");
            long tableRows = rs.getLong("table_rows");
            double dataMb = rs.getDouble("data_mb");
            double indexMb = rs.getDouble("index_mb");
            double totalTableMb = rs.getDouble("total_mb");
            tableData.append("| ").append(tableName)
                    .append(" | ").append(tableRows)
                    .append(" | ").append(String.format("%.2f", dataMb))
                    .append(" | ").append(String.format("%.2f", indexMb))
                    .append(" | ").append(String.format("%.2f", totalTableMb))
                    .append(" |\n");
            if (totalTableMb > 10_240) {
                tableData.append("  ⚠️  **Notable: ").append(tableName).append(" exceeds 10 GB**\n");
            }
            totalMb += totalTableMb;
            tableCount++;
        }
        tableData.append("\n**Total (top ").append(tableCount).append(" tables)**: ")
                .append(String.format("%.2f GB", totalMb / 1024))
                .append(" `[Source: MySQL MCP (").append(config.runtime.mysqlHost).append('-').append(config.runtime.mysqlDB)
                .append(") → SELECT SUM(data_length+index_length) FROM information_schema.tables WHERE table_schema='")
                .append(config.runtime.mysqlDB).append("']`\n");
        return new int[]{tableCount};
    }

    private StringBuilder handleException(ReviewContext context, SQLException e, long startMs, ServiceConfig config) {
        StringBuilder tableData;
        long durationMs = System.currentTimeMillis() - startMs;
        String msg = "NOT_COLLECTED: MySQL MCP error — " + e.getMessage();
        tableData = new StringBuilder(msg);

        auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                "MYSQL_TABLE_QUERY", "MySQLTableStage",
                config.runtime.mysqlHost + "/" + config.runtime.mysqlDB,
                "Top 20 tables by storage",
                null, 500, durationMs, null, e.getMessage(), false));

        logger.error("MySQL table query failed for {}/{}: {}", config.runtime.mysqlHost, config.runtime.mysqlDB, e.getMessage());
        return tableData;
    }
}