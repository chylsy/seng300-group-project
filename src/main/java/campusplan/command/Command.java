package campusplan.command;

public interface Command<T> {
    T execute();
    void undo();
    void redo();
    String description();
}
