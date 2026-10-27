package campusplan.persistence;

import campusplan.domain.CampusPlanData;

/**
 * Persistence abstraction for application data (FR-03).
 * Represents the Strategy pattern for interchangeable storage backends
 * (e.g., InMemoryDataStore for unit tests, JsonDataStore for the real application).
 */
public interface DataStore {

    /**
     * Checks if the backing storage currently exists and contains saved data.
     *
     * @return {@code true} if backing storage exists; {@code false} otherwise.
     */
    boolean exists();

    /**
     * Loads all application data from the backing store.
     *
     * @return the loaded data; never {@code null}
     */
    CampusPlanData load();

    /**
     * Persists all application data to the backing store, replacing previously stored state.
     *
     * @param data the data to persist; must not be {@code null}
     */
    void save(CampusPlanData data);
}
