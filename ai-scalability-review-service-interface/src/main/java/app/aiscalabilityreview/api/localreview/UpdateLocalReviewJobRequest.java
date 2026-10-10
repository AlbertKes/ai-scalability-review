package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;

import java.util.List;

/** Edits the human-owned fields of a run: its note, its labels and whether it is the baseline. */
public class UpdateLocalReviewJobRequest {
    @Property(name = "note")
    public String note;

    @Property(name = "labels")
    public List<String> labels;

    @Property(name = "baseline")
    public Boolean baseline;
}
