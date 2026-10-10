package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import app.aiscalabilityreview.domain.embedded.LocalReviewRequestSnapshot;

/**
 * Converts the trigger request between its three shapes: the incoming API request, the snapshot
 * stored with the run, and the view the console renders.
 *
 * <p>Three shapes rather than one because the incoming request carries {@code @NotNull} contracts
 * that a historical run or a half-filled preset must not be held to when it is read back.
 */
public final class LocalReviewRequestMapper {
    public static LocalReviewRequestSnapshot toSnapshot(GenerateLocalReviewRequest request) {
        LocalReviewRequestSnapshot snapshot = new LocalReviewRequestSnapshot();
        snapshot.service = request.service;
        snapshot.localAppRepoPath = request.localAppRepoPath;
        snapshot.localInfraRepoPath = request.localInfraRepoPath;
        snapshot.env = request.env;
        snapshot.namespace = request.namespace;
        snapshot.domain = request.domain;
        snapshot.mysqlHost = request.mysqlHost;
        snapshot.mysqlDB = request.mysqlDB;
        snapshot.atlasCluster = request.atlasCluster;
        snapshot.azureResourceGroup = request.azureResourceGroup;
        snapshot.azureSubscriptionId = request.azureSubscriptionId;
        snapshot.hpaType = request.hpaType;
        snapshot.kafkaConsumerGroups = request.kafkaConsumerGroups;
        snapshot.skipCodeAnalysis = request.skipCodeAnalysis;
        snapshot.skipValidation = request.skipValidation;
        snapshot.appCodePaths = request.appCodePaths;
        snapshot.codeAnalysisModel = request.codeAnalysisModel;
        snapshot.metricCollectionModel = request.metricCollectionModel;
        snapshot.synthesisModel = request.synthesisModel;
        snapshot.validationModel = request.validationModel;
        snapshot.outputBaseDir = request.outputBaseDir;
        snapshot.operator = request.operator;
        return snapshot;
    }

    public static LocalReviewRequestSnapshot toSnapshot(LocalReviewRequestView view) {
        return toSnapshot(toRequest(view));
    }

    public static GenerateLocalReviewRequest toRequest(LocalReviewRequestSnapshot snapshot) {
        GenerateLocalReviewRequest request = new GenerateLocalReviewRequest();
        request.service = snapshot.service;
        request.localAppRepoPath = snapshot.localAppRepoPath;
        request.localInfraRepoPath = snapshot.localInfraRepoPath;
        request.env = snapshot.env;
        request.namespace = snapshot.namespace;
        request.domain = snapshot.domain;
        request.mysqlHost = snapshot.mysqlHost;
        request.mysqlDB = snapshot.mysqlDB;
        request.atlasCluster = snapshot.atlasCluster;
        request.azureResourceGroup = snapshot.azureResourceGroup;
        request.azureSubscriptionId = snapshot.azureSubscriptionId;
        request.hpaType = snapshot.hpaType;
        request.kafkaConsumerGroups = snapshot.kafkaConsumerGroups;
        request.skipCodeAnalysis = snapshot.skipCodeAnalysis;
        request.skipValidation = snapshot.skipValidation;
        request.appCodePaths = snapshot.appCodePaths;
        request.codeAnalysisModel = snapshot.codeAnalysisModel;
        request.metricCollectionModel = snapshot.metricCollectionModel;
        request.synthesisModel = snapshot.synthesisModel;
        request.validationModel = snapshot.validationModel;
        request.outputBaseDir = snapshot.outputBaseDir;
        request.operator = snapshot.operator;
        return request;
    }

    public static GenerateLocalReviewRequest toRequest(LocalReviewRequestView view) {
        GenerateLocalReviewRequest request = new GenerateLocalReviewRequest();
        request.service = view.service;
        request.localAppRepoPath = view.localAppRepoPath;
        request.localInfraRepoPath = view.localInfraRepoPath;
        request.env = view.env;
        request.namespace = view.namespace;
        request.domain = view.domain;
        request.mysqlHost = view.mysqlHost;
        request.mysqlDB = view.mysqlDB;
        request.atlasCluster = view.atlasCluster;
        request.azureResourceGroup = view.azureResourceGroup;
        request.azureSubscriptionId = view.azureSubscriptionId;
        request.hpaType = view.hpaType;
        request.kafkaConsumerGroups = view.kafkaConsumerGroups;
        request.skipCodeAnalysis = view.skipCodeAnalysis;
        request.skipValidation = view.skipValidation;
        request.appCodePaths = view.appCodePaths;
        request.codeAnalysisModel = view.codeAnalysisModel;
        request.metricCollectionModel = view.metricCollectionModel;
        request.synthesisModel = view.synthesisModel;
        request.validationModel = view.validationModel;
        request.outputBaseDir = view.outputBaseDir;
        request.operator = view.operator;
        return request;
    }

    public static LocalReviewRequestView toView(LocalReviewRequestSnapshot snapshot) {
        if (snapshot == null) return null;
        LocalReviewRequestView view = new LocalReviewRequestView();
        view.service = snapshot.service;
        view.localAppRepoPath = snapshot.localAppRepoPath;
        view.localInfraRepoPath = snapshot.localInfraRepoPath;
        view.env = snapshot.env;
        view.namespace = snapshot.namespace;
        view.domain = snapshot.domain;
        view.mysqlHost = snapshot.mysqlHost;
        view.mysqlDB = snapshot.mysqlDB;
        view.atlasCluster = snapshot.atlasCluster;
        view.azureResourceGroup = snapshot.azureResourceGroup;
        view.azureSubscriptionId = snapshot.azureSubscriptionId;
        view.hpaType = snapshot.hpaType;
        view.kafkaConsumerGroups = snapshot.kafkaConsumerGroups;
        view.skipCodeAnalysis = snapshot.skipCodeAnalysis;
        view.skipValidation = snapshot.skipValidation;
        view.appCodePaths = snapshot.appCodePaths;
        view.codeAnalysisModel = snapshot.codeAnalysisModel;
        view.metricCollectionModel = snapshot.metricCollectionModel;
        view.synthesisModel = snapshot.synthesisModel;
        view.validationModel = snapshot.validationModel;
        view.outputBaseDir = snapshot.outputBaseDir;
        view.operator = snapshot.operator;
        return view;
    }

    private LocalReviewRequestMapper() {
    }
}
