package campusplan.domain;

//three stages an event can exist as
public enum EventStatus {
    DRAFT,      // planned, no room or time picked yet
    SCHEDULED,  // room and time chosen
    CANCELLED   // event called off, kept on record
}
