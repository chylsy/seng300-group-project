package campusplan.domain;

import campusplan.error.NotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CampusPlanData {
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<String, StudentAvailability> availability = new LinkedHashMap<>();
    private final Map<String, Event> events = new LinkedHashMap<>();


    public Map<String, Student> getStudents() { return students; }
    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, StudentAvailability> getAvailability() { return availability; }
    public Map<String, Event> getEvents() { return events; }

    public boolean isEmpty() {
        return students.isEmpty() && rooms.isEmpty() && availability.isEmpty() && events.isEmpty();
    }

    public Optional<Student> findByEmail(String email) {
        String key = Validation.requireEmail(email);
        return students.values().stream().filter(s -> s.emailKey().equals(key)).findFirst();
    }

    public StudentAvailability getStudentAvailability(String studentId) {
        String id = Validation.requireText(studentId, 1, 100, "studentId");
        if (!students.containsKey(id)) {
            throw new NotFoundException("Student", id);
        }
        return availability.computeIfAbsent(id, StudentAvailability::new);
    }

    public CampusPlanData deepCopy() {
        CampusPlanData copy = new CampusPlanData();
        students.forEach((id, s) -> copy.students.put(id, new Student(s.getId(), s.getName(), s.getEmail())));
        rooms.forEach((id, r) -> copy.rooms.put(id,
                new Room(r.getId(), r.getName(), r.getCapacity(), r.getFeatures())));
        availability.forEach((id, a) -> {
            StudentAvailability cloned = new StudentAvailability(a.getStudentId());
            for (TimeInterval interval : a.getIntervals()) {
                cloned.add(new TimeInterval(interval.getDate(), interval.getStart(), interval.getEnd()));
            }
            copy.availability.put(id, cloned);
        });
        events.forEach((id, e) -> copy.events.put(id, new Event(e)));
        return copy;
    }


    public void loadSeedIfEmpty(CampusPlanData source) {
        Validation.require(isEmpty(), "seed", "Seed data can only be loaded into an empty application.");
        Validation.require(source != null, "seed", "Seed data is required.");
        CampusPlanData copy = source.deepCopy();
        students.putAll(copy.students);
        rooms.putAll(copy.rooms);
        availability.putAll(copy.availability);
        events.putAll(copy.events);
    }
}
