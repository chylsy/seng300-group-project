package campusplan.service;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventStatus;
import campusplan.domain.Room;
import campusplan.domain.Suggestion;
import campusplan.domain.SuggestionResult;
import campusplan.domain.TimeInterval;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the ranked suggestions for one event (FR-04, section 3 of the outline).
 * 1.Room checks come first: the room must hold every invitee, provide every required feature and have no overlapping SCHEDULED event. A failure counts in rejectedForRoom. Adjacent times do not overlap.
 * 2.Then each invitee is checked by the EligibilityRule. Fewer eligible than the minimum attendance counts in rejectedForAttendance.
 * 3. Valid candidates are ranked by eligible count (high first), date and start time (early first), unused seats (fewest first) and room ID (A first). The top five are returned.
 */
public class SuggestionGenerator {
    private static final int STEP_MINUTES = 30;

    //candidate that passed both checks. unusedSeats is capacity minus the number of invitees
    private record Candidate(String roomId,TimeInterval slot,int unusedSeats,List<String> eligibleIds, Map<String, String> ineligibleReasons) { }

    private static final Comparator<Candidate> RANKING =(a,b) ->{
        int byCount=Integer.compare(b.eligibleIds().size(), a.eligibleIds().size());
        if (byCount !=0){
            return byCount;
        }
        int byDate=a.slot().getDate().compareTo(b.slot().getDate());
        if (byDate !=0){
            return byDate;
        }
        int byStart=a.slot().getStart().compareTo(b.slot().getStart());
        if (byStart !=0){
            return byStart;
        }
        int bySeats=Integer.compare(a.unusedSeats(), b.unusedSeats());
        if (bySeats !=0) {
            return bySeats;
        }
        return a.roomId().compareTo(b.roomId());
    };

    private final CampusPlanData data;
    private final EligibilityRule rule;

    public SuggestionGenerator(CampusPlanData data, EligibilityRule rule) {
        this.data=Objects.requireNonNull(data,"data");
        this.rule =Objects.requireNonNull(rule,"rule");
    }

    public SuggestionResult generate(Event event){
        Objects.requireNonNull(event, "event");

        List<Room> rooms=new ArrayList<>(data.getRooms().values());
        rooms.sort(Comparator.comparing(Room::getId));
        Map<String, List<TimeInterval>> booked =bookedSlotsByRoom(event);
        int duration =event.getDurationMinutes();
        int open =Room.OPEN.toSecondOfDay()/60;
        int close=Room.CLOSE.toSecondOfDay()/60;
        List<Candidate> valid =new ArrayList<>();
        int rejectedForRoom =0;
        int rejectedForAttendance =0;

        for (LocalDate day=event.getWindowStart(); !day.isAfter(event.getWindowEnd());day=day.plusDays(1)){
            for (int minute=open; minute+duration <= close; minute +=STEP_MINUTES) {
                LocalTime start = LocalTime.of(minute /60,minute%60);
                TimeInterval slot = new TimeInterval(day, start, start.plusMinutes(duration));
                for (Room room :rooms){
                    if (!roomFits(room, event) || isBooked(booked.get(room.getId()), slot)){
                        rejectedForRoom++;
                        continue;
                    }

                    Candidate candidate =evaluate(event, room, slot);
                    if (candidate.eligibleIds().size()< event.getMinAttendance()) {
                        rejectedForAttendance++;
                    }
                    else{
                        valid.add(candidate);
                    }
            }
            }
        }
        valid.sort(RANKING);
        List<Suggestion> top =new ArrayList<>();
        for (Candidate c :valid.subList(0, Math.min(SuggestionResult.MAX_SUGGESTIONS, valid.size()))) {
            top.add(new Suggestion(c.roomId(), c.slot(), c.eligibleIds(), c.ineligibleReasons()));
        }
        return new SuggestionResult(top, rejectedForRoom,rejectedForAttendance);
    }

    private static boolean roomFits(Room room, Event event){
        return room.canHold(event.getInviteeIds().size()) && room.hasAll(event.getRequiredFeatures());
    }

    //asks the rule about every invitee, keeping the invitee order
    private Candidate evaluate(Event event, Room room, TimeInterval slot) {
        List<String> eligible =new ArrayList<>();
        Map<String, String> reasons=new LinkedHashMap<>();

        for (String studentId:event.getInviteeIds()) {
            String reason=rule.ineligibleReason(event,studentId,slot);
            if (reason==null){
                eligible.add(studentId);
            }
            else{
                reasons.put(studentId, reason);
            }
        }
        int unusedSeats =room.getCapacity() -event.getInviteeIds().size();
        return new Candidate(room.getId(), slot, unusedSeats,eligible,reasons);
    }

    //SCHEDULED events grouped by room, built once per call; event being planned is left out
    private Map<String, List<TimeInterval>> bookedSlotsByRoom(Event planned) {
        Map<String, List<TimeInterval>> booked =new HashMap<>();
        for (Event other:data.getEvents().values()) {
            if (other.getId().equals(planned.getId())){
                continue;
            }
            if (other.getStatus()==EventStatus.SCHEDULED && other.getRoomId() != null && other.getSlot() != null) {
                booked.computeIfAbsent(other.getRoomId(), k -> new ArrayList<>()).add(other.getSlot());
            }
        }
        return booked;
    }

    private static boolean isBooked(List<TimeInterval> bookedSlots, TimeInterval slot) {
        if (bookedSlots==null) {
            return false;
        }
        for (TimeInterval b :bookedSlots) {
            if (b.overlaps(slot)) {
                return true;
            }
        }
        return false;
    }
}