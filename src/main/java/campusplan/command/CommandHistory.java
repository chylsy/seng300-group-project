package campusplan.command;

import campusplan.domain.CampusPlanData;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public final class CommandHistory {
    private static final int LIMIT = 20;
    private final CampusPlanData data;
    private final Deque<Command<?>> undo = new ArrayDeque<>();
    private final Deque<Command<?>> redo = new ArrayDeque<>();

    public CommandHistory(CampusPlanData data) {
        this.data = data;
    }

    public <T> T run(String description, Supplier<T> mutation) {
        Command<T> command = new SnapshotCommand<>(data, description, mutation);
        T result = command.execute();
        undo.push(command);
        if (undo.size() > LIMIT) {
            undo.removeLast();
        }
        redo.clear();
        return result;
    }

    public void run(String description, Runnable mutation) {
        run(description, () -> {
            mutation.run();
            return null;
        });
    }

    public boolean undo() {
        if (undo.isEmpty()) return false;
        undo.peek().undo();
        redo.push(undo.pop());
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) return false;
        redo.peek().redo();
        undo.push(redo.pop());
        return true;
    }
}
