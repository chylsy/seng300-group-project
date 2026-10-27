package campusplan.domain;

import java.util.Objects;
import java.util.Locale;


 /*
 rules for P1:
 - name has to be 1-10 chars after trimming 
 - email has 1 "@" with text on both sides no whitespace
 - id is assigned and never changed 
 - no deletion  
 */
public class Student {

    private final String id; //read only
    private String name;
     private String email;


     public Student(String id, String name, String email){
        if (id == null || id.isBlank()){
            throw new IllegalArgumentException("Student ID cannot be null.");

        }
        this.id = id;
        setName(name);
        setEmail(email);
     }
     /*getters*/
     public String getId(){
        return id;
     }
     public String getName(){
        return name;
     }
     public String getEmail(){
        return email;
     }
    /*validate before modifying */

    public void setName(String name){
        /*check if name is valid, if not then current name is left as is*/
        this.name = Validation.requireText(name, 1, 100, "name");
        
        
    }
    public  void setEmail(String email){
        /*set email after trimming and checking format. if email is not valid current email is unchanged */
        this.email = Validation.requireEmail(email);
        
    }
    /*normalize email for duplicate checking*/
    public String emailKey(){
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Override 
    public boolean equals(Object o){
        if (this == o){
            return true;
        }
        if (!(o instanceof Student)){
            return false;
        }
        Student other = (Student) o;
        //compare ids
        return id.equals(other.id);
    }
    @Override 
    public int hashCode(){
        return Objects.hash(id);

    }
    @Override 
    public String toString(){
        return "[" + name + ":" + id + "|" + email + "]";
        
    }

}
