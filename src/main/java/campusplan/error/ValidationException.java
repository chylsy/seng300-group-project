package campusplan.error;

//thrown when a field breaks a validation rule
public class ValidationException extends CampusPlanException{
    private final String field;

    public ValidationException(String field,String message) {
        super(message);
        this.field=field;
    }

    public String getField() {
        return field;
    }


}