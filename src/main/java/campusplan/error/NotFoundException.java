package campusplan.error;

//thrown when an id does not match any record
public class NotFoundException extends CampusPlanException {
    public NotFoundException(String message) {
        super(message);
    }

    //builds the message from what was missing and its id, e.g. "Student not found: s9"
    public NotFoundException(String entity, String id) {
        super(entity + " not found: " + id);
    }
}