package app.aiscalabilityreview.exception;

import java.io.Serial;

public class FatalStageException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 8505790086035892759L;
    public final String stageName;

    public FatalStageException(String stageName, Throwable cause) {
        super("Fatal failure in stage " + stageName + ": " + cause.getMessage(), cause);
        this.stageName = stageName;
    }
}
