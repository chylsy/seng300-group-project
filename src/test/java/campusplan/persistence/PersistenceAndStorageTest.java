package campusplan.persistence;

import campusplan.domain.*;
import campusplan.error.CorruptDataException;
import campusplan.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceAndStorageTest {

    @TempDir
    Path tempDir;

    private CampusPlanData sampleData;

    @BeforeEach
    void setUp() {
        sampleData = new CampusPlanData();

        Student s1 = new Student("S001", "Alice Smith", "alice@example.com");
        Student s2 = new Student("S002", "Bob Jones", "bob@example.com");
        sampleData.getStudents().put(s1.getId(), s1);
        sampleData.getStudents().put(s2.getId(), s2);

        Room r1 = new Room("R001", "Boardroom", 10, EnumSet.of(RoomFeature.WHITEBOARD, RoomFeature.PROJECTOR));
        sampleData.getRooms().put(r1.getId(), r1);

        StudentAvailability sa = new StudentAvailability("S001");
        sa.add(new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(12, 0)));
        sampleData.getAvailability().put("S001", sa);
    }

    // =========================================================================
    // InMemoryDataStore Tests
    // =========================================================================

    @Test
    void inMemoryStore_initiallyDoesNotExist() {
        InMemoryDataStore store = new InMemoryDataStore();
        assertFalse(store.exists());
        assertTrue(store.load().isEmpty());
    }

    @Test
    void inMemoryStore_saveAndLoad_preservesDataAndIsolatesCopies() {
        InMemoryDataStore store = new InMemoryDataStore();
        store.save(sampleData);

        assertTrue(store.exists());
        CampusPlanData loaded = store.load();

        assertEquals(2, loaded.getStudents().size());
        assertEquals("Alice Smith", loaded.getStudents().get("S001").getName());
        assertEquals(1, loaded.getRooms().size());
        assertEquals(1, loaded.getAvailability().size());

        // Mutating loaded data must not mutate stored data
        loaded.getStudents().clear();
        CampusPlanData reloaded = store.load();
        assertEquals(2, reloaded.getStudents().size(), "Mutating loaded copy should not mutate stored snapshot");
    }

    // =========================================================================
    // JsonDataStore Tests
    // =========================================================================

    @Test
    void jsonDataStore_missingFile_returnsFalseForExistsAndEmptyOnLoad() {
        Path jsonFile = tempDir.resolve("missing.json");
        JsonDataStore store = new JsonDataStore(jsonFile);

        assertFalse(store.exists());
        CampusPlanData loaded = store.load();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void jsonDataStore_saveAndLoad_roundTripSuccess() {
        Path jsonFile = tempDir.resolve("campusplan.json");
        JsonDataStore store = new JsonDataStore(jsonFile);

        store.save(sampleData);
        assertTrue(store.exists());
        assertTrue(Files.exists(jsonFile));

        CampusPlanData loaded = store.load();
        assertEquals(2, loaded.getStudents().size());
        assertEquals("Alice Smith", loaded.getStudents().get("S001").getName());
        assertEquals("alice@example.com", loaded.getStudents().get("S001").getEmail());

        Room loadedRoom = loaded.getRooms().get("R001");
        assertNotNull(loadedRoom);
        assertEquals("Boardroom", loadedRoom.getName());
        assertEquals(10, loadedRoom.getCapacity());
        assertTrue(loadedRoom.getFeatures().contains(RoomFeature.WHITEBOARD));
        assertTrue(loadedRoom.getFeatures().contains(RoomFeature.PROJECTOR));

        StudentAvailability loadedSa = loaded.getAvailability().get("S001");
        assertNotNull(loadedSa);
        assertEquals(1, loadedSa.getIntervals().size());
    }

    @Test
    void jsonDataStore_atomicWrite_overwritesCleanly() {
        Path jsonFile = tempDir.resolve("atomic.json");
        JsonDataStore store = new JsonDataStore(jsonFile);

        store.save(sampleData);
        long size1 = jsonFile.toFile().length();
        assertTrue(size1 > 0);

        // Add another student and save again
        sampleData.getStudents().put("S003", new Student("S003", "Charlie Brown", "charlie@example.com"));
        store.save(sampleData);

        CampusPlanData loaded = store.load();
        assertEquals(3, loaded.getStudents().size());
        assertEquals("Charlie Brown", loaded.getStudents().get("S003").getName());
    }

    @Test
    void jsonDataStore_corruptJson_throwsCorruptDataException() throws IOException {
        Path jsonFile = tempDir.resolve("corrupt.json");
        Files.writeString(jsonFile, "{ this is not valid json :;;; ");

        JsonDataStore store = new JsonDataStore(jsonFile);
        assertTrue(store.exists());

        CorruptDataException ex = assertThrows(CorruptDataException.class, store::load);
        assertEquals(jsonFile, ex.getPath());
        assertNotNull(ex.getDetail());
    }

    @Test
    void jsonDataStore_missingSchemaVersion_throwsCorruptDataException() throws IOException {
        Path jsonFile = tempDir.resolve("no_version.json");
        Files.writeString(jsonFile, "{\"students\":[],\"rooms\":[],\"availability\":[],\"events\":[]}");

        JsonDataStore store = new JsonDataStore(jsonFile);
        assertThrows(CorruptDataException.class, store::load);
    }

    @Test
    void jsonDataStore_wrongSchemaVersion_throwsCorruptDataException() throws IOException {
        Path jsonFile = tempDir.resolve("wrong_version.json");
        Files.writeString(jsonFile, "{\"schemaVersion\":99,\"students\":[],\"rooms\":[],\"availability\":[],\"events\":[]}");

        JsonDataStore store = new JsonDataStore(jsonFile);
        assertThrows(CorruptDataException.class, store::load);
    }

    @Test
    void jsonDataStore_duplicateStudentId_throwsCorruptDataException() throws IOException {
        Path jsonFile = tempDir.resolve("dup_student.json");
        String badJson = """
                {
                  "schemaVersion": 1,
                  "students": [
                    {"id": "S001", "name": "Alice", "email": "alice1@example.com"},
                    {"id": "S001", "name": "Alice Two", "email": "alice2@example.com"}
                  ],
                  "rooms": [],
                  "availability": [],
                  "events": []
                }
                """;
        Files.writeString(jsonFile, badJson);

        JsonDataStore store = new JsonDataStore(jsonFile);
        assertThrows(CorruptDataException.class, store::load);
    }

    // =========================================================================
    // StorageService Tests
    // =========================================================================

    @Test
    void storageService_startApp_withExistingStore_loadsData() {
        InMemoryDataStore store = new InMemoryDataStore(sampleData);
        StorageService service = new StorageService(store);

        CampusPlanData activeData = service.startApp();
        assertSame(activeData, service.getData());
        assertEquals(2, activeData.getStudents().size());
    }

    @Test
    void storageService_startApp_withMissingStore_startsEmpty() {
        InMemoryDataStore store = new InMemoryDataStore(); // empty/does not exist
        StorageService service = new StorageService(store);

        CampusPlanData activeData = service.startApp();
        assertNotNull(activeData);
        assertTrue(activeData.isEmpty());
    }

    @Test
    void storageService_save_persistsActiveDataToStore() {
        InMemoryDataStore store = new InMemoryDataStore();
        StorageService service = new StorageService(store);
        service.startApp();

        // Mutate service data
        service.getData().getStudents().put("S010", new Student("S010", "Zoe Vance", "zoe@example.com"));

        assertFalse(store.exists(), "Store should not have data before save");
        service.save();
        assertTrue(store.exists(), "Store should have data after save");

        CampusPlanData loaded = store.load();
        assertEquals(1, loaded.getStudents().size());
        assertEquals("Zoe Vance", loaded.getStudents().get("S010").getName());
    }

    @Test
    void storageService_loadSeed_loadsSampleSeedDataIntoEmptyApp() {
        Path seedPath = Path.of("sample-data", "seed.json");
        if (!Files.exists(seedPath)) {
            return; // Skip if run outside repository root
        }

        InMemoryDataStore store = new InMemoryDataStore();
        StorageService service = new StorageService(store);
        service.startApp();
        assertTrue(service.getData().isEmpty());

        service.loadSeed(seedPath);

        assertFalse(service.getData().isEmpty());
        assertEquals(12, service.getData().getStudents().size());
        assertEquals(6, service.getData().getRooms().size());
    }
}
