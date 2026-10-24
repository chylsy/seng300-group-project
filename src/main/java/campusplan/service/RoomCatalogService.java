package campusplan.service;

import campusplan.domain.CampusPlanData;
import campusplan.domain.Room;
import campusplan.domain.RoomFeature;
import campusplan.domain.Validation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Objects;

/**
 * Read only access to the room catalogue for P1. Also never changes data so a failed call cannot damage records
 * Sorted by ascending room ID.
 */
public class RoomCatalogService {
    private static final Comparator<Room> BY_ID=Comparator.comparing(Room::getId);
    private final CampusPlanData data;

    public RoomCatalogService(CampusPlanData data) {
        this.data=Objects.requireNonNull(data, "data");
    }

    //all rooms in the catalogue sorted by ID, returning a new list each time
    public List<Room> listAll() {
        List<Room> rooms=new ArrayList<>(data.getRooms().values());
        rooms.sort(BY_ID);
        return rooms;
    }

    /**
     * Rooms with at least minCapacity seats that provide every required feature.
     * @param minCapacity minimum seats 1-100
     * @param required features every result must have, null/empty means no feature filter
     */
    public List<Room> filter(int minCapacity, Set<RoomFeature> required) {
        Validation.requireRange(minCapacity, Room.MIN_CAPACITY, Room.MAX_CAPACITY, "minCapacity");
        List<Room> matches=new ArrayList<>();
        for (Room room :data.getRooms().values()) {
            if (room.canHold(minCapacity)&&room.hasAll(required)){
                matches.add(room); //match found
            }

        }
        matches.sort(BY_ID);
        return matches;
    }



}