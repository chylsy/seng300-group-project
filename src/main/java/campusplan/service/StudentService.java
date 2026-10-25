package campusplan.service;

import java.util.List;
import java.util.Objects;

/*rules
- mutate after validation
- throw CampusPlanExeption if invalid
- emails need to be unique
- no deletion
 */

public class StudentService {

    private final CampusPlanData data;
    private final IdGenerator ids;

    public StudentService(CampusPlanData data, IdGenerator ids){
        this.data = Objects.requireNonNull(data, "data");
        this.ids = Objects.requireNonNull(ids, "ids");

    }

    public Student create(String name, String email){

    }
    public Student edit(String id, String name, String email){

    }
    public Student get(String id){

    }
    public List<Student> listAll(){

    }
    public List<Student> search(String text){
        
    }
    
}
