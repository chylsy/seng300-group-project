package campusplan.persistence;

import campusplan.domain.CampusPlanData;
import java.util.Objects;

/**
 * In-memory implementation of {@link DataStore} intended for testing (FR-03).
 * Keeps an isolated deep copy snapshot of {@link CampusPlanData}.
 */
public final class InMemoryDataStore implements DataStore {

    private CampusPlanData saved;

    public InMemoryDataStore() {
        this.saved = null;
    }

    public InMemoryDataStore(CampusPlanData initial) {
        if (initial != null) {
            this.saved = initial.deepCopy();
        }
    }

    @Override
    public boolean exists() {
        return saved != null;
    }

    @Override
    public CampusPlanData load() {
        if (saved == null) {
            return new CampusPlanData();
        }
        return saved.deepCopy();
    }

    @Override
    public void save(CampusPlanData data) {
        Objects.requireNonNull(data, "data must not be null");
        this.saved = data.deepCopy();
    }
}
