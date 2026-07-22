package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

public class GenerateLocalReviewResponse {
    @NotNull
    @NotBlank
    @Property(name = "job_id")
    public String jobId;  // poll /review/job/:jobId for status

    @NotNull
    @NotBlank
    @Property(name = "output_dir")
    public String outputDir;  // absolute path where output files will be saved
}
