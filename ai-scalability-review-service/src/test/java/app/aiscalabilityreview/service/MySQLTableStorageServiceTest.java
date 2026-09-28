package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.MySQLTableStorageService.MySQLTableParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MySQLTableStorageServiceTest {
    @TempDir
    Path tempDir;

    private MySQLTableStorageService service;
    private String outputPath;

    @BeforeEach
    void createMySQLTableStorageService() {
        service = new MySQLTableStorageService();
        outputPath = tempDir.resolve("mysql-tables.md").toString();
    }

    /**
     * The metric collection stage reads this file unconditionally, so a service without MySQL must
     * still leave one behind — otherwise the stage spends a tool call discovering it is missing.
     */
    @Test
    void writesANotCollectedFileWhenTheServiceHasNoMysql() throws IOException {
        String summary = service.collect(new MySQLTableParams("N/A", "N/A", outputPath));

        assertThat(summary).startsWith("NOT_COLLECTED");
        assertThat(Files.readString(Path.of(outputPath)))
            .startsWith("NOT_COLLECTED: MySQL not configured for this service");
    }

    @Test
    void treatsABlankHostTheSameAsAnAbsentOne() throws IOException {
        service.collect(new MySQLTableParams(null, "wonder_cart", outputPath));

        assertThat(Files.readString(Path.of(outputPath))).startsWith("NOT_COLLECTED");
    }

    /**
     * An unresolvable host must degrade to a NOT_COLLECTED line rather than throw, because the
     * infra context stage is fatal and a database that needs the VPN is a routine local condition.
     */
    @Test
    void reportsAConnectionFailureAsNotCollected() throws IOException {
        String summary = service.collect(new MySQLTableParams("no-such-host.invalid", "wonder_cart", outputPath));

        assertThat(summary).startsWith("NOT_COLLECTED");
        assertThat(Files.readString(Path.of(outputPath))).startsWith("NOT_COLLECTED: MySQL");
    }
}
