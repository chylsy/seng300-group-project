package campusplan.persistence;

import campusplan.domain.*;
import campusplan.error.CorruptDataException;
import campusplan.error.StorageException;
import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * {@link DataStore} implementation that reads and writes {@link CampusPlanData} as a JSON file.
 * <p>
 * Key behaviors specified in UML and P1 constraints:
 * <ul>
 *   <li>Atomic writes ({@link #writeAtomically(String)}): saves to a temporary file before renaming,
 *       ensuring a failed write leaves the previous valid file intact.</li>
 *   <li>Corrupt file handling: throwing {@link CorruptDataException} when encountering corrupt or
 *       malformed data.</li>
 *   <li>Existence check ({@link #exists()}): verifies whether the backing file exists.</li>
 * </ul>
 * </p>
 */
public final class JsonDataStore implements DataStore {

    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .setPrettyPrinting()
            .create();

    private final Path file;

    /**
     * Constructs a JsonDataStore targeting the specified file path.
     *
     * @param file path to the persistent JSON file
     */
    public JsonDataStore(Path file) {
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    /**
     * Checks if the backing JSON file exists.
     *
     * @return {@code true} if the file exists and is a regular file
     */
    @Override
    public boolean exists() {
        return Files.exists(file) && Files.isRegularFile(file);
    }

    /**
     * Loads application data from the persistent JSON file.
     * <p>
     * If the file does not exist, an empty {@link CampusPlanData} is returned.
     * If the file exists but contains corrupted, invalid, or schema-violating data,
     * {@link CorruptDataException} is thrown.
     * </p>
     *
     * @return the loaded dataset
     * @throws CorruptDataException if the file exists but cannot be parsed or validated
     * @throws StorageException     if an I/O error occurs while reading the file
     */
    @Override
    public CampusPlanData load() {
        if (!exists()) {
            return new CampusPlanData();
        }

        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException(file, "Could not read data file " + file + ": " + e.getMessage(), e);
        }

        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null || !root.isJsonObject()) {
                throw new CorruptDataException(file, "JSON root is not an object.");
            }
            return deserialize(root);
        } catch (CorruptDataException e) {
            throw e;
        } catch (JsonParseException | DateTimeParseException | IllegalArgumentException e) {
            throw new CorruptDataException(file, "Invalid or malformed JSON data: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new CorruptDataException(file, "Unexpected error parsing data: " + e.getMessage(), e);
        }
    }

    /**
     * Persists application data to the backing file atomically.
     *
     * @param data the dataset to persist; must not be {@code null}
     * @throws StorageException if the file cannot be written
     */
    @Override
    public void save(CampusPlanData data) {
        Objects.requireNonNull(data, "data must not be null");
        String json = GSON.toJson(serialize(data));
        writeAtomically(json);
    }

    /**
     * Writes the given JSON string to a temporary file in the target directory and
     * atomically replaces the destination file.
     *
     * @param json JSON payload to persist
     */
    private void writeAtomically(String json) {
        Path parent = file.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new StorageException(file, "Could not create directory for " + file + ": " + e.getMessage(), e);
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(parent != null ? parent : Path.of("."), "campusplan-", ".tmp");
            Files.writeString(tempFile, json, StandardCharsets.UTF_8);

            try {
                Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException(file, "Failed to atomically save file " + file + ": " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Serialization
    // -------------------------------------------------------------------------

    private JsonObject serialize(CampusPlanData data) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.add("students", serializeStudents(data));
        root.add("rooms", serializeRooms(data));
        root.add("availability", serializeAvailability(data));
        root.add("events", serializeEvents(data));
        return root;
    }

    private JsonArray serializeStudents(CampusPlanData data) {
        JsonArray arr = new JsonArray();
        for (Student s : data.getStudents().values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", s.getId());
            o.addProperty("name", s.getName());
            o.addProperty("email", s.getEmail());
            arr.add(o);
        }
        return arr;
    }

    private JsonArray serializeRooms(CampusPlanData data) {
        JsonArray arr = new JsonArray();
        for (Room r : data.getRooms().values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.getId());
            o.addProperty("name", r.getName());
            o.addProperty("capacity", r.getCapacity());
            JsonArray features = new JsonArray();
            for (RoomFeature f : r.getFeatures()) {
                features.add(f.name());
            }
            o.add("features", features);
            arr.add(o);
        }
        return arr;
    }

    private JsonArray serializeAvailability(CampusPlanData data) {
        JsonArray arr = new JsonArray();
        for (StudentAvailability sa : data.getAvailability().values()) {
            JsonObject o = new JsonObject();
            o.addProperty("studentId", sa.getStudentId());
            JsonArray intervals = new JsonArray();
            for (TimeInterval ti : sa.getIntervals()) {
                JsonObject iv = new JsonObject();
                iv.addProperty("date", ti.getDate().toString());
                iv.addProperty("start", ti.getStart().toString());
                iv.addProperty("end", ti.getEnd().toString());
                intervals.add(iv);
            }
            o.add("intervals", intervals);
            arr.add(o);
        }
        return arr;
    }

    private JsonArray serializeEvents(CampusPlanData data) {
        JsonArray arr = new JsonArray();
        // Uses reflection/generic access to Event map to remain resilient
        // across unmerged branch states
        Map<?, ?> eventMap = data.getEvents();
        if (eventMap != null) {
            for (Object obj : eventMap.values()) {
                if (obj == null) continue;
                try {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", (String) obj.getClass().getMethod("getId").invoke(obj));
                    o.addProperty("title", (String) obj.getClass().getMethod("getTitle").invoke(obj));
                    
                    @SuppressWarnings("unchecked")
                    List<String> inviteeIds = (List<String>) obj.getClass().getMethod("getInviteeIds").invoke(obj);
                    JsonArray invitees = new JsonArray();
                    if (inviteeIds != null) {
                        for (String id : inviteeIds) invitees.add(id);
                    }
                    o.add("inviteeIds", invitees);

                    Object ws = obj.getClass().getMethod("getWindowStart").invoke(obj);
                    Object we = obj.getClass().getMethod("getWindowEnd").invoke(obj);
                    if (ws != null) o.addProperty("windowStart", ws.toString());
                    if (we != null) o.addProperty("windowEnd", we.toString());

                    o.addProperty("durationMinutes", (Integer) obj.getClass().getMethod("getDurationMinutes").invoke(obj));

                    @SuppressWarnings("unchecked")
                    Set<RoomFeature> rf = (Set<RoomFeature>) obj.getClass().getMethod("getRequiredFeatures").invoke(obj);
                    JsonArray requiredFeatures = new JsonArray();
                    if (rf != null) {
                        for (RoomFeature f : rf) requiredFeatures.add(f.name());
                    }
                    o.add("requiredFeatures", requiredFeatures);

                    o.addProperty("minAttendance", (Integer) obj.getClass().getMethod("getMinAttendance").invoke(obj));
                    Object st = obj.getClass().getMethod("getStatus").invoke(obj);
                    if (st != null) o.addProperty("status", st.toString());

                    @SuppressWarnings("unchecked")
                    Map<String, String> responses = (Map<String, String>) obj.getClass().getMethod("getResponses").invoke(obj);
                    JsonObject respObj = new JsonObject();
                    if (responses != null) {
                        for (Map.Entry<String, String> entry : responses.entrySet()) {
                            respObj.addProperty(entry.getKey(), entry.getValue());
                        }
                    }
                    o.add("responses", respObj);

                    String roomId = (String) obj.getClass().getMethod("getRoomId").invoke(obj);
                    if (roomId != null) o.addProperty("roomId", roomId);

                    Object slot = obj.getClass().getMethod("getSlot").invoke(obj);
                    if (slot instanceof TimeInterval ti) {
                        JsonObject slotObj = new JsonObject();
                        slotObj.addProperty("date", ti.getDate().toString());
                        slotObj.addProperty("start", ti.getStart().toString());
                        slotObj.addProperty("end", ti.getEnd().toString());
                        o.add("slot", slotObj);
                    }

                    arr.add(o);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // Deserialization
    // -------------------------------------------------------------------------

    private CampusPlanData deserialize(JsonObject root) {
        if (!root.has("schemaVersion") || root.get("schemaVersion").isJsonNull()) {
            throw new CorruptDataException(file, "Missing schemaVersion.");
        }
        int version = integer(root, "schemaVersion");
        if (version != SCHEMA_VERSION) {
            throw new CorruptDataException(file, "Unsupported schemaVersion: " + version + "; expected " + SCHEMA_VERSION);
        }

        CampusPlanData data = new CampusPlanData();
        deserializeStudents(array(root, "students"), data);
        deserializeRooms(array(root, "rooms"), data);
        deserializeAvailability(array(root, "availability"), data);
        deserializeEvents(array(root, "events"), data);
        return data;
    }

    private void deserializeStudents(JsonArray rows, CampusPlanData data) {
        Set<String> emails = new HashSet<>();
        for (JsonElement row : rows) {
            JsonObject o = object(row, "student");
            Student student = new Student(id(o), text(o, "name"), text(o, "email"));
            if (data.getStudents().containsKey(student.getId())) {
                throw new CorruptDataException(file, "Duplicate student ID: " + student.getId());
            }
            if (!emails.add(student.emailKey())) {
                throw new CorruptDataException(file, "Duplicate student email: " + student.getEmail());
            }
            data.getStudents().put(student.getId(), student);
        }
    }

    private void deserializeRooms(JsonArray rows, CampusPlanData data) {
        Set<String> names = new HashSet<>();
        for (JsonElement row : rows) {
            JsonObject o = object(row, "room");
            Room room = new Room(id(o), text(o, "name"), integer(o, "capacity"), features(o));
            if (data.getRooms().containsKey(room.getId())) {
                throw new CorruptDataException(file, "Duplicate room ID: " + room.getId());
            }
            if (!names.add(room.nameKey())) {
                throw new CorruptDataException(file, "Duplicate room name: " + room.getName());
            }
            data.getRooms().put(room.getId(), room);
        }
    }

    private void deserializeAvailability(JsonArray rows, CampusPlanData data) {
        for (JsonElement row : rows) {
            JsonObject o = object(row, "availability");
            String studentId = Validation.requireText(text(o, "studentId"), 1, 100, "studentId");
            if (!data.getStudents().containsKey(studentId)) {
                throw new CorruptDataException(file, "Unknown student in availability: " + studentId);
            }
            if (data.getAvailability().containsKey(studentId)) {
                throw new CorruptDataException(file, "Duplicate availability for student: " + studentId);
            }
            StudentAvailability sa = new StudentAvailability(studentId);
            for (JsonElement item : array(o, "intervals")) {
                JsonObject iv = object(item, "interval");
                LocalDate date = LocalDate.parse(text(iv, "date"));
                LocalTime start = LocalTime.parse(text(iv, "start"));
                LocalTime end = LocalTime.parse(text(iv, "end"));
                Validation.onHalfHour(start);
                Validation.onHalfHour(end);
                Validation.require(start.isBefore(end), "interval", "Start must precede end.");
                Validation.require(!start.isBefore(Room.OPEN) && !end.isAfter(Room.CLOSE),
                        "interval", "Availability must be within 08:00-22:00.");
                sa.add(new TimeInterval(date, start, end));
            }
            data.getAvailability().put(studentId, sa);
        }
    }

    private void deserializeEvents(JsonArray rows, CampusPlanData data) {
        // Safe deserialization using reflection if Event classes exist on runtime classpath
        for (JsonElement row : rows) {
            JsonObject o = object(row, "event");
            String eventId = id(o);
            try {
                Class<?> reqClass = Class.forName("campusplan.domain.EventDraftRequest");
                Class<?> eventClass = Class.forName("campusplan.domain.Event");

                String title = text(o, "title");
                List<String> inviteeIds = new ArrayList<>();
                for (JsonElement v : array(o, "inviteeIds")) {
                    inviteeIds.add(Validation.requireText(v.getAsString(), 1, 100, "inviteeId"));
                }
                LocalDate ws = LocalDate.parse(text(o, "windowStart"));
                LocalDate we = LocalDate.parse(text(o, "windowEnd"));
                int duration = integer(o, "durationMinutes");
                Set<RoomFeature> reqFeat = features(o);
                int minAtt = integer(o, "minAttendance");

                Object req = reqClass.getConstructor(
                        String.class, List.class, LocalDate.class, LocalDate.class, int.class, Set.class, int.class
                ).newInstance(title, inviteeIds, ws, we, duration, reqFeat, minAtt);

                Object event = eventClass.getConstructor(String.class, reqClass).newInstance(eventId, req);

                @SuppressWarnings("unchecked")
                Map<String, String> responses = (Map<String, String>) eventClass.getMethod("getResponses").invoke(event);
                if (responses != null) {
                    responses.clear();
                    if (o.has("responses") && o.get("responses").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> entry : o.getAsJsonObject("responses").entrySet()) {
                            responses.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) (Map<?, ?>) data.getEvents();
                if (eventMap != null) {
                    eventMap.put(eventId, event);
                }
            } catch (ClassNotFoundException e) {
                // Event domain classes not yet integrated in this branch
                break;
            } catch (ReflectiveOperationException e) {
                throw new CorruptDataException(file, "Failed to instantiate event: " + e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper parsers
    // -------------------------------------------------------------------------

    private static String id(JsonObject o) {
        return Validation.requireText(text(o, "id"), 1, 100, "id");
    }

    private static Set<RoomFeature> features(JsonObject o) {
        Set<RoomFeature> result = EnumSet.noneOf(RoomFeature.class);
        JsonArray array = o.has("requiredFeatures") ? o.getAsJsonArray("requiredFeatures") :
                (o.has("features") ? o.getAsJsonArray("features") : new JsonArray());
        for (JsonElement v : array) {
            RoomFeature feature = RoomFeature.valueOf(v.getAsString());
            if (!result.add(feature)) {
                throw new IllegalArgumentException("Duplicate room feature: " + feature);
            }
        }
        return result;
    }

    private static JsonElement required(JsonObject o, String field) {
        JsonElement value = o.get(field);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("Field '" + field + "' is required.");
        }
        return value;
    }

    private static String text(JsonObject o, String field) {
        return string(required(o, field), field);
    }

    private static String string(JsonElement value, String field) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Field '" + field + "' must be a string.");
        }
        return value.getAsString();
    }

    private static int integer(JsonObject o, String field) {
        JsonElement value = required(o, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Field '" + field + "' must be an integer.");
        }
        return value.getAsBigDecimal().intValueExact();
    }

    private static JsonArray array(JsonObject o, String field) {
        JsonElement value = required(o, field);
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException("Field '" + field + "' must be an array.");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject object(JsonElement value, String field) {
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("Field '" + field + "' must be an object.");
        }
        return value.getAsJsonObject();
    }
}
