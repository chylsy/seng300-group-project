package campusplan.error;

public final class DuplicateEmailException extends CampusPlanException {
    public DuplicateEmailException(String email) {
        super("A student already uses email: " + email);
    }
}
