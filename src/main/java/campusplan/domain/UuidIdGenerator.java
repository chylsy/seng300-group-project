package campusplan.domain;

import java.util.UUID;

public final class UuidIdGenerator implements IdGenerator {
    @Override
    public String next(String prefix) {
        String checked = Validation.requireText(prefix, 1, 40, "prefix");
        Validation.require(checked.codePoints().noneMatch(Validation::isWhitespace),
                "prefix", "ID prefix must not contain whitespace.");
        return checked + "-" + UUID.randomUUID();
    }
}
