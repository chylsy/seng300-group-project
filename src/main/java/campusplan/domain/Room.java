package campusplan.domain;

import campusplan.ValidationException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Campus room (room editing is out of scope)
 * Rules: immutable ID, name 1-60 characters after trimming, capacity 1-100, any subset of RoomFeature. Rooms are open daily from
 * OPEN to CLOSE.
 */
public final class Room {

    public static final LocalTime OPEN =LocalTime.of(8,0);
    public static final LocalTime CLOSE=LocalTime.of(22,0);

    public static final int MAX_NAME_LENGTH=60;
    public static final int MIN_CAPACITY=1;
    public static final int MAX_CAPACITY= 100;

    private final String id;
    private final String name;
    private final int capacity;
    private final Set<RoomFeature> features;

    public Room(String id, String name, int capacity, Set<RoomFeature> features) {
        this.id =Validation.requireText(id, 1, 100, "id");
        this.name=Validation.requireText(name, 1, MAX_NAME_LENGTH, "name");
        this.capacity=Validation.requireRange(capacity, MIN_CAPACITY, MAX_CAPACITY, "capacity");
        this.features=copyFeatures(features);
    }

    private static Set<RoomFeature> copyFeatures(Set<RoomFeature> features) {
        if (features ==null){
            throw new ValidationException("features","Features must not be null. Use an empty set for no features.");
        }
        EnumSet<RoomFeature> copy=EnumSet.noneOf(RoomFeature.class);
        for (RoomFeature feature:features){
            if (feature==null){
                throw new ValidationException("features", "Features must not contain null.");
            }
            copy.add(feature);
        }
        return Collections.unmodifiableSet(copy);
    }

    //true when room has enough seats for this many people
    public boolean canHold(int people){
        return people <=capacity;
    }

    //true when the room provides every required feature, null/empty means no requirement
    public boolean hasAll(Set<RoomFeature> required) {
        return required ==null||features.containsAll(required);
    }

    //key for the unique name case, the name is already trimmed
    public String nameKey(){
        return name.toLowerCase(Locale.ROOT);
    }
    public String getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public int getCapacity(){
        return capacity;
    }

    public Set<RoomFeature> getFeatures(){
        return features;
    }

    @Override
    public boolean equals(Object o){
        if (this==o) {
            return true;
        }
        //check if the other object is null or not even a Room
        if (o ==null ||getClass()!=o.getClass()){
            return false;
        }
        //cast the object to a Room so we can access its fields
        Room other = (Room) o;
        //compare their IDs
        return id.equals(other.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    @Override
    public String toString() {
        return name + " (id " +id + ",capacity "+ capacity+ ",features " + (features.isEmpty()? "none":features) + ")";
    }
}