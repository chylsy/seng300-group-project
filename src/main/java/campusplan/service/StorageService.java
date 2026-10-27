package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Validation;
import campusplan.persistence.DataStore;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Application service coordinating persistence and startup data loading (FR-03).
 * <p>
 * According to P1 design:
 * <ul>
 *   <li>On startup ({@link #startApp()}):
 *       <ul>
 *         <li>If saved persistent data exists in {@link DataStore}, loads and returns it.</li>
 *         <li>If no persistent data exists (missing file), starts with an empty dataset so
 *             that initial seed data can be loaded via {@link #loadSeed(Path)}.</li>
 *       </ul>
 *   </li>
 *   <li>{@link #save()}: Persists the current in-memory dataset to the backing {@link DataStore}.</li>
 *   <li>{@link #loadSeed(Path)}: Populates an empty application dataset from a seed JSON file.</li>
 * </ul>
 * </p>
 */
public class StorageService {

    private final DataStore store;
    private CampusPlanData data;

    /**
     * Constructs a StorageService with the given backing store and an initial empty dataset.
     *
     * @param store the data store to persist via; must not be {@code null}
     */
    public StorageService(DataStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.data = new CampusPlanData();
    }

    /**
     * Constructs a StorageService with the given backing store and application dataset.
     *
     * @param store the data store to persist via; must not be {@code null}
     * @param data  the application data container; must not be {@code null}
     */
    public StorageService(DataStore store, CampusPlanData data) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.data = Objects.requireNonNull(data, "data must not be null");
    }

    /**
     * Starts the application storage lifecycle.
     * <p>
     * If backing storage exists, loads and returns the stored {@link CampusPlanData}.
     * If backing storage does not exist (e.g. fresh installation / missing file),
     * starts with an empty {@link CampusPlanData} so the user may load seed data.
     * </p>
     *
     * @return the active application data
     */
    public CampusPlanData startApp() {
        if (store.exists()) {
            this.data = store.load();
        } else {
            this.data = new CampusPlanData();
        }
        return this.data;
    }

    /**
     * Persists the current application data to the backing store.
     */
    public void save() {
        store.save(this.data);
    }

    /**
     * Loads seed data into the application dataset from the specified file.
     * Can only be performed when the application dataset is empty.
     *
     * @param seedFile the path to the seed JSON file; must not be {@code null}
     */
    public void loadSeed(Path seedFile) {
        Validation.require(seedFile != null, "seedFile", "Seed file path is required.");
        new SeedDataLoader().loadIntoEmpty(seedFile, this.data);
    }

    /**
     * Returns the active application data.
     *
     * @return current CampusPlanData instance
     */
    public CampusPlanData getData() {
        return this.data;
    }
}
