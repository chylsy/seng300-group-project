package campusplan.error;

//base class for all CampusPlan errors
public abstract class CampusPlanException extends RuntimeException {
    protected CampusPlanException(String message) {
        super(message);
    }
}