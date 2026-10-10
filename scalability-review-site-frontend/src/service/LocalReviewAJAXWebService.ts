import {ajax} from "@wonder/core-fe";
import {
  GenerateLocalReviewRequest,
  GenerateLocalReviewResponse,
  LocalReviewJobActionResponse,
  LocalReviewRequestView,
  PreflightRequest,
  PreflightResponse,
  RetryLocalReviewRequest,
  UpdateLocalReviewJobRequest,
  ValidateLocalReviewResponse,
} from "type/api";

export class LocalReviewAJAXWebService {
  /**
   * Returns every problem with the request at once, and creates nothing. The form calls this before
   * every submit, which is what makes "fix one list" possible instead of "discover the next problem
   * after each retry".
   */
  static validateRequest(request: LocalReviewRequestView, signal?: AbortSignal): Promise<ValidateLocalReviewResponse> {
    return ajax("POST", "/local-review/validate", {}, request, {signal});
  }

  static generate(request: GenerateLocalReviewRequest): Promise<GenerateLocalReviewResponse> {
    return ajax("POST", "/local-review/generate", {}, request);
  }

  static cancel(jobId: string): Promise<LocalReviewJobActionResponse> {
    return ajax("POST", "/local-review/job/:jobId/cancel", {jobId}, null);
  }

  static retry(jobId: string, request: RetryLocalReviewRequest): Promise<GenerateLocalReviewResponse> {
    return ajax("POST", "/local-review/job/:jobId/retry", {jobId}, request);
  }

  static update(jobId: string, request: UpdateLocalReviewJobRequest): Promise<LocalReviewJobActionResponse> {
    return ajax("PUT", "/local-review/job/:jobId", {jobId}, request);
  }

  static delete(jobId: string): Promise<LocalReviewJobActionResponse> {
    return ajax("DELETE", "/local-review/job/:jobId", {jobId}, null);
  }

  static preflight(request: PreflightRequest, signal?: AbortSignal): Promise<PreflightResponse> {
    return ajax("GET", "/local-review/preflight", {}, request, {signal});
  }
}
