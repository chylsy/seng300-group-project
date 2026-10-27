package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.IdGenerator;
import campusplan.domain.Student;
import campusplan.domain.Validation;
import campusplan.error.DuplicateEmailException;
import campusplan.error.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/*rules
- mutate after validation
- throw CampusPlanExeption if invalid
- emails need to be unique
- no deletion
 */

public class StudentService {

    private static final String ID_PREFIX = "S"; //student id's start with: S

    private final CampusPlanData data;
    private final IdGenerator ids;

    public StudentService(CampusPlanData data, IdGenerator ids){
        this.data = Objects.requireNonNull(data, "data");
        this.ids = Objects.requireNonNull(ids, "ids");

    }

    public Student create(String name, String email){
        /*validate first */
        String validName = Validation.requireText(name, 1, 100, "name");
        String validEmail = Validation.requireEmail(email);
        
        /*reject duplicates */
        Optional<Student> existing = data.findByEmail(validEmail);
        if (existing.isPresent()){
            throw new DuplicateEmailException(validEmail);
        }
        String id = ids.next(ID_PREFIX);
        Student student = new Student(id, validName, validEmail);
        getStudentMap().put(id, student);
        return student;


    }
    public Student edit(String id, String name, String email){
        Student student = get(id);

        String validName = Validation.requireText(name, 1, 100, "name");
        String validEmail = Validation.requireEmail(email);

        /*make sure nobody else is using the same email */
        Optional<Student> existing = data.findByEmail(validEmail);
        if(existing.isPresent()){
            Student owner = existing.get();
            boolean isSameStudent = owner.getId().equals(student.getId());
            if (!isSameStudent){
                throw new DuplicateEmailException(validEmail);
            }
        }
        student.setName(validName);
        student.setEmail(validEmail);
        return student;

    }
    public Student get(String id){
        /*throw NotFound if no matches with this id */
        if(id == null){
            throw new NotFoundException("Student", id);
        }
        /*remove extra whitespace */
        String validId = id.trim();

        Student student = getStudentMap().get(validId);
        if (student == null){
            throw new NotFoundException("Student", id);
        }
        return student; 

    }/* return student list sorted by name
        changing the list does not change stored students */
    public List<Student> listAll(){
        List<Student> result = new ArrayList<>();
        for (Student student : getStudentMap().values()) {
            result.add(student);
        }
 
        sortByNameThenId(result);
        return result;

    }/*search by name or email */
    public List<Student> search(String text){
        if (text == null || text.isBlank()) {

            /*empty text returns all students */
            return listAll();
        }
 
        /*ignore the case when comparing */
        String searchText = text.trim().toLowerCase();
 
        List<Student> result = new ArrayList<>();
        for (Student student : getStudentMap().values()) {
            String name = student.getName().toLowerCase();
            String email = student.getEmail().toLowerCase();
 
            if (name.contains(searchText) || email.contains(searchText)) {
                result.add(student);
            }
        }
 
        sortByNameThenId(result);
        return result;
        
    }
    /*helper methods */
    /*map where students are stored */
    private Map<String, Student> getStudentMap() {
        return data.getStudents();
    }

    private void sortByNameThenId(List<Student> list) {
        /*if two students have the same name, the one with smaller id comes first */
        list.sort((a, b) -> compareByNameThenId(a, b));
    }
    /*decides which student comes first. return a negative num if student a comes first, positive if student b comes first, 0 if same */
    private int compareByNameThenId(Student a, Student b) {
        int nameResult = a.getName().compareToIgnoreCase(b.getName());
        if (nameResult != 0) {
            return nameResult;                  // names are diff, the name decides
        }
        return a.getId().compareTo(b.getId());  // same name, the id decides
    }
}
