package campusplan.service;
import campusplan.domain.CampusPlanData;
import campusplan.domain.Event;
import campusplan.domain.EventStatus;
import campusplan.domain.SuggestionResult;
import campusplan.error.NotFoundException;
import campusplan.error.ValidationException;
import java.util.Objects;

//scheduling operations
public class SchedulingService {
    private final CampusPlanData data;
    private final SuggestionGenerator generator;

    public SchedulingService(CampusPlanData data,EligibilityRule rule) {
        this.data=Objects.requireNonNull(data,"data");
        this.generator=new SuggestionGenerator(data,rule);
    }

    //top five room and time suggestions for an event
    public SuggestionResult suggest(String eventId) {
        String id=eventId ==null ?null :eventId.trim();
        Event event = id ==null ? null :data.getEvents().get(id);

        if (event ==null){
            throw new NotFoundException("Event",eventId);
        }
        if (event.getStatus()==EventStatus.CANCELLED) {
            throw new ValidationException("status","Cancelled events cannot be scheduled");
        }
        return generator.generate(event);
    }

}