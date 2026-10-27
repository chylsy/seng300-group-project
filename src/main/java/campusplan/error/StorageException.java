package campusplan.error;

import java.nio.file.Path;

public class StorageException extends CampusPlanException {
    private final Path path;

    public StorageException(Path path, String message, Throwable cause) {
        super(message);
        this.path = path;
        if (cause != null) {
            initCause(cause);
        }
    }

    public Path getPath() { return path; }
}
