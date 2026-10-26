package campusplan.error;

//thrown when an id does not match any record
public class NotFoundException extends CampusPlanException {
    public NotFoundException(String message) {
        super(message);
    }
}
