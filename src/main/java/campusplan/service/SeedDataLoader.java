package campusplan.service;

import campusplan.domain.*;
import campusplan.error.StorageException;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public final class SeedDataLoader {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    public CampusPlanData loadSeedData(Path path) {
        return loadFromJson(path);
    }

    public CampusPlanData loadFromJson(Path path) {
        Validation.require(path != null, "path", "Seed file path is required.");
        try {
            JsonObject root = JSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            Validation.require(root != null, "seed", "Seed JSON must contain an object.");
            Validation.require(integer(root, "schemaVersion") == 1,
                    "schemaVersion", "Unsupported seed schema version; expected 1.");
            CampusPlanData data = new CampusPlanData();
            loadStudents(array(root, "students"), data);
            loadRooms(array(root, "rooms"), data);
            loadAvailability(array(root, "availability"), data);
            loadEvents(array(root, "events"), data);
            return data;
        } catch (IOException | RuntimeException error) {
            throw new StorageException(path, "Could not load seed file " + path + ": " + error.getMessage(), error);
        }
    }

    public void loadIntoEmpty(Path path, CampusPlanData target) {
        Validation.require(target != null, "data", "Application data is required.");
        Validation.require(target.isEmpty(), "seed", "Seed data can only be loaded into an empty application.");
        CampusPlanData staged = loadFromJson(path);
        target.loadSeedIfEmpty(staged);
    }

    private static void loadStudents(JsonArray rows, CampusPlanData data) {
        Set<String> emails = new HashSet<>();
        for (JsonElement row : rows) {
            JsonObject o = object(row, "student");
            Student student = new Student(id(o), text(o, "name"), text(o, "email"));
            Validation.require(!data.getStudents().containsKey(student.getId()), "id", "Duplicate student ID.");
            Validation.require(emails.add(student.emailKey()), "email", "Duplicate student email.");
            data.getStudents().put(student.getId(), student);
        }
    }

    private static void loadRooms(JsonArray rows, CampusPlanData data) {
        Set<String> names = new HashSet<>();
        for (JsonElement row : rows) {
            JsonObject o = object(row, "room");
            Room room = new Room(id(o), text(o, "name"), integer(o, "capacity"), features(o, "features"));
            Validation.require(!data.getRooms().containsKey(room.getId()), "id", "Duplicate room ID.");
            Validation.require(names.add(room.nameKey()), "name", "Duplicate room name.");
            data.getRooms().put(room.getId(), room);
        }
    }

    private static void loadAvailability(JsonArray rows, CampusPlanData data) {
        for (JsonElement row : rows) {
            JsonObject o = object(row, "availability");
            String studentId = Validation.requireText(text(o, "studentId"), 1, 100, "studentId");
            Validation.require(data.getStudents().containsKey(studentId), "studentId", "Unknown student in availability.");
            Validation.require(!data.getAvailability().containsKey(studentId), "studentId", "Duplicate availability record.");
            StudentAvailability availability = new StudentAvailability(studentId);
            for (JsonElement item : array(o, "intervals")) {
                JsonObject interval = object(item, "interval");
                LocalDate date = LocalDate.parse(text(interval, "date"));
                LocalTime start = LocalTime.parse(text(interval, "start"));
                LocalTime end = LocalTime.parse(text(interval, "end"));
                Validation.onHalfHour(start);
                Validation.onHalfHour(end);
                Validation.require(start.isBefore(end), "interval", "Start must precede end.");
                Validation.require(!start.isBefore(Room.OPEN) && !end.isAfter(Room.CLOSE),
                        "interval", "Availability must be within 08:00-22:00.");
                availability.add(new TimeInterval(date, start, end));
            }
            data.getAvailability().put(studentId, availability);
        }
    }

    private static void loadEvents(JsonArray rows, CampusPlanData data) {
        CampusPlanData validationData = new CampusPlanData();
        validationData.getStudents().putAll(data.getStudents());
        EventPlanningService validator = new EventPlanningService(validationData);
        for (JsonElement row : rows) {
            JsonObject o = object(row, "event");
            String id = id(o);
            Validation.require(!data.getEvents().containsKey(id), "id", "Duplicate event ID.");
            Validation.require(text(o, "status").equals("DRAFT"), "status", "Seed events must be DRAFT.");
            List<String> invitees = new ArrayList<>();
            for (JsonElement value : array(o, "inviteeIds")) {
                invitees.add(Validation.requireText(string(value, "inviteeId"), 1, 100, "inviteeId"));
            }
            EventDraftRequest request = new EventDraftRequest(text(o, "title"), invitees,
                    LocalDate.parse(text(o, "windowStart")), LocalDate.parse(text(o, "windowEnd")),
                    integer(o, "durationMinutes"), features(o, "requiredFeatures"), integer(o, "minAttendance"));
            validator.createDraft(request);
            Event event = new Event(id, request);
            for (String studentId : invitees) {
                event.getResponses().put(studentId, "PENDING");
            }
            data.getEvents().put(id, event);
        }
    }

    private static String id(JsonObject o) {
        return Validation.requireText(text(o, "id"), 1, 100, "id");
    }

    private static Set<RoomFeature> features(JsonObject o, String field) {
        Set<RoomFeature> result = EnumSet.noneOf(RoomFeature.class);
        for (JsonElement value : array(o, field)) {
            RoomFeature feature = RoomFeature.valueOf(string(value, field));
            Validation.require(result.add(feature), field, "Duplicate room feature.");
        }
        return result;
    }

    private static JsonElement required(JsonObject o, String field) {
        JsonElement value = o.get(field);
        Validation.require(value != null && !value.isJsonNull(), field, field + " is required.");
        return value;
    }

    private static String text(JsonObject o, String field) {
        return string(required(o, field), field);
    }

    private static String string(JsonElement value, String field) {
        Validation.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(),
                field, field + " must be a string.");
        return value.getAsString();
    }

    private static int integer(JsonObject o, String field) {
        JsonElement value = required(o, field);
        Validation.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(),
                field, field + " must be an integer.");
        return value.getAsBigDecimal().intValueExact();
    }

    private static JsonArray array(JsonObject o, String field) {
        JsonElement value = required(o, field);
        Validation.require(value.isJsonArray(), field, field + " must be an array.");
        return value.getAsJsonArray();
    }

    private static JsonObject object(JsonElement value, String field) {
        Validation.require(value.isJsonObject(), field, field + " must be an object.");
        return value.getAsJsonObject();
    }
}
