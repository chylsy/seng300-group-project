package campusplan.command;

import campusplan.domain.CampusPlanData;
import java.util.function.Supplier;

/** Captures both sides so replay preserves generated IDs and related records. */
final class SnapshotCommand<T> implements Command<T> {
    private final CampusPlanData data;
    private final String description;
    private final Supplier<T> mutation;
    private CampusPlanData before;
    private CampusPlanData after;

    SnapshotCommand(CampusPlanData data, String description, Supplier<T> mutation) {
        this.data = data;
        this.description = description;
        this.mutation = mutation;
    }

    public T execute() {
        before = data.deepCopy();
        try {
            T result = mutation.get();
            after = data.deepCopy();
            return result;
        } catch (RuntimeException failure) {
            data.restoreFrom(before);
            throw failure;
        }
    }

    public void undo() { data.restoreFrom(before); }
    public void redo() { data.restoreFrom(after); }
    public String description() { return description; }
}
