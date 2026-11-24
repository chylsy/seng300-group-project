package campusplan.domain;
import java.util.List;

//The outcome of one suggestion request (FR-04): up to five ranked suggestions, plus how many candidates were rejected
//Room failures are counted first, so the two counts never overlap

public final class SuggestionResult {
    public static final int MAX_SUGGESTIONS=5;
    private final List<Suggestion> top;
    private final int rejectedForRoom;
    private final int rejectedForAttendance;

    public SuggestionResult(List<Suggestion> top, int rejectedForRoom,int rejectedForAttendance){
        Validation.require(top!=null,"top", "The suggestion list is required.");
        Validation.require(top.size() <=MAX_SUGGESTIONS, "top", "At most "+MAX_SUGGESTIONS+ " suggestions are allowed.");
        Validation.require(rejectedForRoom >=0, "rejectedForRoom", "A count cannot be negative.");
        Validation.require(rejectedForAttendance >=0, "rejectedForAttendance", "A count cannot be negative.");
        this.top=List.copyOf(top);
        this.rejectedForRoom=rejectedForRoom;
        this.rejectedForAttendance=rejectedForAttendance;
    }

    public List<Suggestion> getTop(){
        return top;
    }
    public int getRejectedForRoom(){
        return rejectedForRoom;
    }
    public int getRejectedForAttendance(){
        return rejectedForAttendance;
    }
    public boolean hasSuggestions() {
        return !top.isEmpty();
    }

}