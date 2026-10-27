package campusplan;

import campusplan.domain.*;
import campusplan.error.*;
import campusplan.service.SeedDataLoader;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SeedDataLoaderTest {
    @TempDir Path temp;
    private final SeedDataLoader loader = new SeedDataLoader();
    private final Path seed = Path.of("sample-data/seed.json");

    private JsonObject fixture() throws Exception {
        return JsonParser.parseString(Files.readString(seed)).getAsJsonObject();
    }

    private void rejectsWithoutChanges(Consumer<JsonObject> mutation) throws Exception {
        JsonObject json = fixture();
        mutation.accept(json);
        Path path = temp.resolve("bad.json");
        String contents = json.toString();
        Files.writeString(path, contents, StandardCharsets.UTF_8);
        CampusPlanData target = new CampusPlanData();
        StorageException e = assertThrows(StorageException.class, () -> loader.loadIntoEmpty(path, target));
        assertEquals(path, e.getPath());
        assertNotNull(e.getCause());
        assertTrue(target.isEmpty());
        assertEquals(contents, Files.readString(path));
    }

    @Test void fixtureMeetsCountsAndKeepsIdsAndPendingResponses() {
        CampusPlanData data = loader.loadSeedData(seed);
        assertEquals(12, data.getStudents().size());
        assertEquals(6, data.getRooms().size());
        assertEquals(3, data.getEvents().size());
        Set<LocalDate> dates = new HashSet<>();
        data.getAvailability().values().forEach(a -> a.getIntervals().forEach(i -> dates.add(i.getDate())));
        assertEquals(7, dates.size());
        assertEquals("Avery Chen", data.getStudents().get("S001").getName());
        for (Event e : data.getEvents().values()) {
            assertEquals(EventStatus.DRAFT, e.getStatus());
            assertNull(e.getRoomId());
            assertNull(e.getSlot());
            assertEquals(new HashSet<>(e.getInviteeIds()), e.getResponses().keySet());
            assertTrue(e.getResponses().values().stream().allMatch("PENDING"::equals));
        }
    }

    @Test void explicitLoadOnlyRunsOnAnEmptyTarget() {
        CampusPlanData target = new CampusPlanData();
        loader.loadIntoEmpty(seed, target);
        assertThrows(ValidationException.class, () -> loader.loadIntoEmpty(temp.resolve("missing"), target));
        assertEquals(12, target.getStudents().size());
    }

    @Test void missingFileIsReportedAndNeverCreated() {
        Path missing = temp.resolve("missing.json");
        CampusPlanData target = new CampusPlanData();
        assertThrows(StorageException.class, () -> loader.loadIntoEmpty(missing, target));
        assertFalse(Files.exists(missing));
        assertTrue(target.isEmpty());
    }

    @Test void corruptAndNonStandardJsonNeverChangesTheTargetOrFile() throws Exception {
        for (String contents : List.of("{", "null", "[]", "{\"schemaVersion\":1} trailing", "/*comment*/{}")) {
            Path path = temp.resolve("corrupt.json");
            Files.writeString(path, contents);
            CampusPlanData target = new CampusPlanData();
            assertThrows(StorageException.class, () -> loader.loadIntoEmpty(path, target));
            assertTrue(target.isEmpty());
            assertEquals(contents, Files.readString(path));
        }
    }

    @Test void unsupportedVersionAndMissingArraysAreRejected() throws Exception {
        rejectsWithoutChanges(j -> j.addProperty("schemaVersion", 2));
        rejectsWithoutChanges(j -> j.remove("students"));
        rejectsWithoutChanges(j -> j.add("events", JsonNull.INSTANCE));
    }

    @Test void duplicateEmailsAndStudentIdsAreRejected() throws Exception {
        rejectsWithoutChanges(j -> j.getAsJsonArray("students").get(1).getAsJsonObject().addProperty("email", " STUDENT01@EXAMPLE.TEST "));
        rejectsWithoutChanges(j -> j.getAsJsonArray("students").get(1).getAsJsonObject().addProperty("id", "S001"));
    }

    @Test void duplicateRoomNamesAndIdsAreRejected() throws Exception {
        rejectsWithoutChanges(j -> j.getAsJsonArray("rooms").get(1).getAsJsonObject().addProperty("name", " CEDAR STUDY ROOM "));
        rejectsWithoutChanges(j -> j.getAsJsonArray("rooms").get(1).getAsJsonObject().addProperty("id", "R001"));
    }

    @Test void invalidRoomTypesAndFeaturesAreRejected() throws Exception {
        rejectsWithoutChanges(j -> j.getAsJsonArray("rooms").get(0).getAsJsonObject().addProperty("capacity", 4.5));
        rejectsWithoutChanges(j -> j.getAsJsonArray("rooms").get(0).getAsJsonObject().addProperty("capacity", "4"));
        rejectsWithoutChanges(j -> j.getAsJsonArray("rooms").get(0).getAsJsonObject().addProperty("capacity", 101));
        rejectsWithoutChanges(j -> j.getAsJsonArray("rooms").get(0).getAsJsonObject().getAsJsonArray("features").add("UNKNOWN"));
    }

    @Test void unknownAndRepeatedAvailabilityOwnersAreRejected() throws Exception {
        rejectsWithoutChanges(j -> j.getAsJsonArray("availability").get(0).getAsJsonObject().addProperty("studentId", "ghost"));
        rejectsWithoutChanges(j -> j.getAsJsonArray("availability").add(j.getAsJsonArray("availability").get(0).deepCopy()));
    }

    @Test void invalidIntervalsAreRejectedEvenThoughTheDomainConstructorDoesNotValidate() throws Exception {
        for (String start : List.of("07:30", "10:15", "10:00:01", "10:00:00.000000001", "16:00", "17:00", "bad")) {
            rejectsWithoutChanges(j -> j.getAsJsonArray("availability").get(0).getAsJsonObject()
                    .getAsJsonArray("intervals").get(0).getAsJsonObject().addProperty("start", start));
        }
        rejectsWithoutChanges(j -> j.getAsJsonArray("availability").get(0).getAsJsonObject()
                .getAsJsonArray("intervals").get(0).getAsJsonObject().addProperty("end", "22:30"));
    }

    @Test void invalidDraftConstraintsAndReferencesAreRejected() throws Exception {
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(0).getAsJsonObject().addProperty("durationMinutes", 45));
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(0).getAsJsonObject().addProperty("minAttendance", 7));
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(0).getAsJsonObject().getAsJsonArray("inviteeIds").add("ghost"));
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(0).getAsJsonObject().getAsJsonArray("inviteeIds").add("S001"));
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(0).getAsJsonObject().addProperty("windowEnd", "2026-11-09"));
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(0).getAsJsonObject().addProperty("status", "SCHEDULED"));
        rejectsWithoutChanges(j -> j.getAsJsonArray("events").get(1).getAsJsonObject().addProperty("id", "EV-001"));
    }

    @Test void adjacentAvailabilityIsMergedAndUtf8NamesSurviveLoading() throws Exception {
        JsonObject json = fixture();
        json.getAsJsonArray("students").get(0).getAsJsonObject().addProperty("name", "Zoë García");
        JsonObject first = json.getAsJsonArray("availability").get(0).getAsJsonObject();
        JsonObject extra = first.getAsJsonArray("intervals").get(0).getAsJsonObject().deepCopy();
        extra.addProperty("start", "16:00");
        extra.addProperty("end", "17:00");
        first.getAsJsonArray("intervals").add(extra);
        Path path = temp.resolve("unicode.json");
        Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
        CampusPlanData data = loader.loadFromJson(path);
        assertEquals("Zoë García", data.getStudents().get("S001").getName());
        assertTrue(data.getStudentAvailability("S001").covers(new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(17, 0))));
    }

    @Test void sampleHasFiveEligibleOfSixAndAlsoAnImpossibleEvent() {
        CampusPlanData data = loader.loadFromJson(seed);
        TimeInterval slot = new TimeInterval(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(12, 0));
        Event event = data.getEvents().get("EV-001");
        assertEquals(5, eligible(data, event, slot));
        assertFalse(data.getStudentAvailability("S006").covers(slot));
        assertTrue(hasCandidate(data, event));
        assertFalse(hasCandidate(data, data.getEvents().get("EV-002")));
    }

    @Test void noSuggestionsFixtureHasNoFeasibleCandidateForAnyEvent() {
        CampusPlanData data = loader.loadFromJson(Path.of("sample-data/no-suggestions.json"));
        assertEquals(12, data.getStudents().size());
        assertEquals(6, data.getRooms().size());
        assertEquals(3, data.getEvents().size());
        for (Event event : data.getEvents().values()) {
            assertFalse(hasCandidate(data, event));
        }
    }


    private long eligible(CampusPlanData data, Event event, TimeInterval slot) {
        return event.getInviteeIds().stream().filter(id -> data.getStudentAvailability(id).covers(slot)).count();
    }

    private boolean hasCandidate(CampusPlanData data, Event event) {
        for (LocalDate day = event.getWindowStart(); !day.isAfter(event.getWindowEnd()); day = day.plusDays(1)) {
            for (int minute = 8 * 60; minute + event.getDurationMinutes() <= 22 * 60; minute += 30) {
                LocalTime start = LocalTime.of(minute / 60, minute % 60);
                TimeInterval slot = new TimeInterval(day, start, start.plusMinutes(event.getDurationMinutes()));
                if (eligible(data, event, slot) < event.getMinAttendance()) continue;
                for (Room room : data.getRooms().values()) {
                    if (room.canHold(event.getInviteeIds().size()) && room.hasAll(event.getRequiredFeatures())) return true;
                }
            }
        }
        return false;
    }
}
