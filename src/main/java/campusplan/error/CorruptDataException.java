package campusplan.error;

import java.nio.file.Path;

/**
 * Thrown when persistent data in storage is malformed, has an invalid schema,
 * or violates domain constraints.
 */
public class CorruptDataException extends StorageException {
    private final String detail;

    public CorruptDataException(Path path, String detail) {
        super(path, "Corrupt data in " + path + ": " + detail, null);
        this.detail = detail;
    }

    public CorruptDataException(Path path, String detail, Throwable cause) {
        super(path, "Corrupt data in " + path + ": " + detail, cause);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
