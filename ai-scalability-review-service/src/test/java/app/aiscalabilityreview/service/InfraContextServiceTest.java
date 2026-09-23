package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.InfraContextService.InfraContextParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InfraContextServiceTest {
    @TempDir
    Path root;

    private Path repo;
    private Path output;

    @BeforeEach
    void createInfraRepo() throws IOException {
        repo = root.resolve("infra");
        output = root.resolve("out/infra-context.md");

        write("prod/app/consumer/kube/resource/22-wonder-cart-service.yml", "kind: Deployment\nname: wonder-cart-service\n");
        write("prod/app/consumer/kube/resource/kafka-consumer/22-cart-kafka.yml", "name: wonder-cart-service-kafka-consumer\n");
        write("prod/app/consumer/kube/resource/190-hpa.yml", "targets:\n  - wonder-cart-service\n");
        write("prod/app/consumer/kube/resource/15-order-service.yml", "kind: Deployment\nname: order-service\n");
        write("prod/app/consumer/kube/resource/README.md", "not a manifest\n");

        write("prod/app/infra/env/12-aks.tf", "resource azurerm_kubernetes_cluster_node_pool {\n  vm_size = D8s\n}\n");
        write("prod/app/infra/env/05-storage.tf", "resource azurerm_storage_account {}\n");
        write("prod/app/infra/env/16-mysql-flexible-wonder-db.tf", "name = rfprodv2-flexible-wonder-db\n");

        write("prod/app/infra/mysql/02-wonder-db.tf", "module wonder-db {}\n");
        write("prod/app/infra/mysql/02-other-db.tf", "module other-db {}\n");
        write("prod/app/infra/mysql/secrets/source/wonder-db.tf", "password = super-secret\n");

        write("prod/atlas/cluster.consumer.tf", "name = consumer-cluster\n");
        write("prod/atlas/cluster.hdr.tf", "name = hdr-cluster\n");
    }

    @Test
    void selectsOnlyTheFilesRelevantToTheService() throws IOException {
        new InfraContextService().build(params("rfprodv2-flexible-wonder-db.mysql.database.azure.com", "consumer-cluster"));
        String document = Files.readString(output);

        assertThat(document)
            .contains("22-wonder-cart-service.yml")
            .contains("kafka-consumer/22-cart-kafka.yml")
            .contains("190-hpa.yml")
            .contains("env/12-aks.tf")
            .contains("16-mysql-flexible-wonder-db.tf")
            .contains("mysql/02-wonder-db.tf")
            .contains("cluster.consumer.tf");
        assertThat(document)
            .doesNotContain("15-order-service.yml")
            .doesNotContain("05-storage.tf")
            .doesNotContain("02-other-db.tf")
            .doesNotContain("cluster.hdr.tf")
            .doesNotContain("README.md");
    }

    @Test
    void neverCopiesFilesOutOfASecretsTree() throws IOException {
        new InfraContextService().build(params("rfprodv2-flexible-wonder-db.mysql.database.azure.com", null));

        assertThat(Files.readString(output)).doesNotContain("super-secret").doesNotContain("secrets/source");
    }

    @Test
    void skipsTheDatabaseSectionsWhenTheServiceUsesNoDatabase() throws IOException {
        new InfraContextService().build(params("N/A", "N/A"));
        String document = Files.readString(output);

        assertThat(document)
            .contains("22-wonder-cart-service.yml")
            .doesNotContain("MySQL database (Terraform)")
            .doesNotContain("Atlas MongoDB (Terraform)");
    }

    @Test
    void fallsBackToEveryManifestWhenTheServiceNameMatchesNothing() throws IOException {
        new InfraContextService().build(new InfraContextParams(repo.toString(), "prod", "consumer",
            "service-that-does-not-exist", "N/A", "N/A", output.toString()));
        String document = Files.readString(output);

        assertThat(document)
            .contains("nothing matched")
            .contains("15-order-service.yml")
            .contains("22-wonder-cart-service.yml");
    }

    @Test
    void reportsMissingDirectoriesInsteadOfFailing() throws IOException {
        new InfraContextService().build(new InfraContextParams(repo.toString(), "uat", "consumer",
            "wonder-cart-service", "N/A", "N/A", output.toString()));

        assertThat(Files.readString(output)).contains("directory not found");
    }

    @Test
    void includesAFileMatchedByTwoCollectionsOnlyOnce() throws IOException {
        write("prod/app/infra/env/20-shared.tf", "node_pool for rfprodv2-flexible-wonder-db\n");
        new InfraContextService().build(params("rfprodv2-flexible-wonder-db.mysql.database.azure.com", null));
        String document = Files.readString(output);

        assertThat(document.split("### `infra/prod/app/infra/env/20-shared.tf`", -1)).hasSize(2);
    }

    private InfraContextParams params(String mysqlHost, String atlasCluster) {
        return new InfraContextParams(repo.toString(), "prod", "consumer", "wonder-cart-service",
            mysqlHost, atlasCluster, output.toString());
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = repo.resolve(relativePath);
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, content);
    }
}
