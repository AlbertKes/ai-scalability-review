package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

public class ListArtifactResponse {
    @Property(name = "output_dir")
    public String outputDir;

    @Property(name = "artifacts")
    public List<ArtifactView> artifacts;

    public static class ArtifactView {
        @Property(name = "name")
        public String name;

        /** Stage that wrote it, or null when it cannot be attributed. */
        @Property(name = "stage")
        public String stage;

        @Property(name = "kind")
        public String kind;  // MARKDOWN | JSON | LOG | OTHER

        @Property(name = "size_bytes")
        public Long sizeBytes;

        @Property(name = "modified_at")
        public ZonedDateTime modifiedAt;
    }
}
