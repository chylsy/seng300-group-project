package campusplan.domain;

@FunctionalInterface
public interface IdGenerator {
    String next(String prefix);

    default String nextId(String prefix) {
        return next(prefix);
    }
}
