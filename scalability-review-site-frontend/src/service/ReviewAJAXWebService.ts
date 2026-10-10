import {ajax} from "@wonder/core-fe";
import {
  CompareRunRequest,
  CompareRunResponse,
  GetReviewJobResponse,
  GetReviewReportResponse,
  GetReviewReportValidationResponse,
  GetStageLogRequest,
  GetStageLogResponse,
  GetUsageRequest,
  GetUsageResponse,
  ListArtifactResponse,
  ListFindingFeedbackResponse,
  ListReviewJobRequest,
  ListReviewJobResponse,
  ListServiceSummaryResponse,
  SaveFindingFeedbackRequest,
} from "type/api";

export class ReviewAJAXWebService {
  static getJob(jobId: string, signal?: AbortSignal): Promise<GetReviewJobResponse> {
    return ajax("GET", "/review/job/:jobId", {jobId}, null, {signal});
  }

  static listJob(request: ListReviewJobRequest, signal?: AbortSignal): Promise<ListReviewJobResponse> {
    return ajax("GET", "/review/job", {}, request, {signal});
  }

  static listArtifact(jobId: string, signal?: AbortSignal): Promise<ListArtifactResponse> {
    return ajax("GET", "/review/job/:jobId/artifact", {jobId}, null, {signal});
  }

  static getStageLog(jobId: string, request: GetStageLogRequest, signal?: AbortSignal): Promise<GetStageLogResponse> {
    return ajax("GET", "/review/job/:jobId/stage-log", {jobId}, request, {signal});
  }

  static compareRun(request: CompareRunRequest, signal?: AbortSignal): Promise<CompareRunResponse> {
    return ajax("GET", "/review/run/compare", {}, request, {signal});
  }

  static usage(request: GetUsageRequest, signal?: AbortSignal): Promise<GetUsageResponse> {
    return ajax("GET", "/review/usage", {}, request, {signal});
  }

  static listService(signal?: AbortSignal): Promise<ListServiceSummaryResponse> {
    return ajax("GET", "/review/service", {}, null, {signal});
  }

  static getReport(reportId: string, signal?: AbortSignal): Promise<GetReviewReportResponse> {
    return ajax("GET", "/review/report/:reportId", {reportId}, null, {signal});
  }

  static getValidation(validationId: string, signal?: AbortSignal): Promise<GetReviewReportValidationResponse> {
    return ajax("GET", "/review/report/validation/:validationId", {validationId}, null, {signal});
  }

  static listFindingFeedback(reportId: string, signal?: AbortSignal): Promise<ListFindingFeedbackResponse> {
    return ajax("GET", "/review/report/:reportId/feedback", {reportId}, null, {signal});
  }

  static saveFindingFeedback(
    reportId: string,
    request: SaveFindingFeedbackRequest
  ): Promise<ListFindingFeedbackResponse> {
    return ajax("POST", "/review/report/:reportId/feedback", {reportId}, request);
  }

  /** Artifacts are files, not beans, so they are fetched by URL rather than through ajax(). */
  static artifactURL(jobId: string, name: string): string {
    return `/review/job/${encodeURIComponent(jobId)}/artifact/${encodeURIComponent(name)}`;
  }

  static archiveURL(jobId: string): string {
    return `/review/job/${encodeURIComponent(jobId)}/artifacts.zip`;
  }
}
