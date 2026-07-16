package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.AuditLogService;
import core.framework.inject.Inject;
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
        if (config.mysqlHost == null || config.mysqlHost.isBlank()
                || config.mysqlDb == null || config.mysqlDb.isBlank()) {
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
        String jdbcUrl = "jdbc:mysql://" + config.mysqlHost + ":" + mysqlPort
                + "/" + config.mysqlDb + "?useSSL=true&requireSSL=false&connectTimeout=10000&socketTimeout=30000";
        logger.info("Querying MySQL table sizes for {}/{}", config.mysqlHost, config.mysqlDb);

        long startMs = System.currentTimeMillis();
        StringBuilder tableData = new StringBuilder();
        tableData.append("## MySQL Table Size Analysis\n\n");
        tableData.append("```\n");
        tableData.append("-- [Source: MySQL MCP (").append(config.mysqlHost).append("-").append(config.mysqlDb).append(") →\n");
        tableData.append("SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb,\n");
        tableData.append("       ROUND(index_length/1024/1024,2) AS index_mb,\n");
        tableData.append("       ROUND((data_length+index_length)/1024/1024,2) AS total_mb\n");
        tableData.append("FROM information_schema.tables WHERE table_schema='").append(config.mysqlDb).append("'\n");
        tableData.append("ORDER BY (data_length+index_length) DESC LIMIT 20;\n");
        tableData.append("-- ]\n```\n\n");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, mysqlUser, mysqlPassword);
             PreparedStatement stmt = conn.prepareStatement(TOP_TABLES_SQL)) {
            stmt.setString(1, config.mysqlDb);
            ResultSet rs = stmt.executeQuery();
            tableData.append("| Table | Rows | Data (MB) | Index (MB) | Total (MB) |\n");
            tableData.append("| :--- | ---: | ---: | ---: | ---: |\n");
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

                totalMb += totalTableMb;
                tableCount++;

                if (totalTableMb > 10_240) {
                    tableData.append("  ⚠️  **Notable: ").append(tableName).append(" exceeds 10 GB**\n");
                }
            }

            tableData.append("\n**Total (top ").append(tableCount).append(" tables)**: ")
                    .append(String.format("%.2f GB", totalMb / 1024))
                    .append(" `[Source: MySQL MCP (").append(config.mysqlHost).append("-").append(config.mysqlDb)
                    .append(") → SELECT SUM(data_length+index_length) FROM information_schema.tables WHERE table_schema='")
                    .append(config.mysqlDb).append("']`\n");

            long durationMs = System.currentTimeMillis() - startMs;
            auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                    "MYSQL_TABLE_QUERY", "MySQLTableStage",
                    config.mysqlHost + "/" + config.mysqlDb,
                    "Top 20 tables by storage",
                    null, 200, durationMs,
                    "Fetched " + tableCount + " rows", null, true));

            logger.info("MySQL table query complete for {}/{}: {} tables, {} GB total",
                    config.mysqlHost, config.mysqlDb, tableCount, String.format("%.2f", totalMb / 1024));

        } catch (SQLException e) {
            tableData = handleException(context, e, startMs, config);
        }
        context.mysqlTableData = tableData.toString();
    }

    private StringBuilder handleException(ReviewContext context, SQLException e, long startMs, ServiceConfig config) {
        StringBuilder tableData;
        long durationMs = System.currentTimeMillis() - startMs;
        String msg = "NOT_COLLECTED: MySQL MCP error — " + e.getMessage();
        tableData = new StringBuilder(msg);

        auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                "MYSQL_TABLE_QUERY", "MySQLTableStage",
                config.mysqlHost + "/" + config.mysqlDb,
                "Top 20 tables by storage",
                null, 500, durationMs, null, e.getMessage(), false));

        logger.error("MySQL table query failed for {}/{}: {}", config.mysqlHost, config.mysqlDb, e.getMessage());
        return tableData;
    }
}