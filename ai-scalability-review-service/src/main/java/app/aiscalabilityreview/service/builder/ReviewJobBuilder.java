package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.api.review.ReviewJobView;
import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import app.aiscalabilityreview.domain.ReviewJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviewJobBuilder {
    public static ReviewJobView reviewJobView(ReviewJob job) {
        var view = new ReviewJobView();
        view.jobId = job.jobId;
        view.serviceId = job.serviceId;
        view.status = job.status;
        view.currentStage = job.currentStage;
        view.aiModel = AIModelView.valueOf(job.aiModel.name());
        view.triggerType = job.triggerType;
        view.note = job.note;
        view.startedAt = job.startedAt;
        view.completedAt = job.completedAt;
        view.reportId = job.reportId;
        view.errorMessage = job.errorMessage;

        if (job.stageStatuses != null && !job.stageStatuses.isEmpty()) {
            List<ReviewJobView.StageView> stages = new ArrayList<>(job.stageStatuses.size());
            for (Map.Entry<String, ReviewJob.StageStatus> entry : job.stageStatuses.entrySet()) {
                var sv = new ReviewJobView.StageView();
                sv.name = entry.getKey();
                ReviewJob.StageStatus ss = entry.getValue();
                if (ss != null) {
                    sv.status = ss.status;
                    sv.durationMs = ss.durationMs;
                    sv.error = ss.errorMessage;
                }
                stages.add(sv);
            }
            view.stages = stages;
        }
        return view;
    }
}
