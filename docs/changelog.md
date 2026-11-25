# Changelog

## Project Milestone 2:

### Delivered features

**FR-04: Scheduling suggestions**
- Added the `Suggestion` and `SuggestionResult` domain classes, holding the room, interval, eligible and ineligible students with reasons, the top five suggestions, and the rejectedForRoom and rejectedForAttendance counts. ([#27](https://github.com/chylsy/seng300-group-project/issues/27))
- Added `SuggestionGenerator`, which tries every 30-minute start in the event's date window against every room, applies room checks first (capacity, required features, overlapping SCHEDULED events), then eligibility checks, and ranks by eligible count, date and time, unused seats, and room ID. ([#30](https://github.com/chylsy/seng300-group-project/issues/30))
- Added `SchedulingService.suggest(eventId)`, which rejects unknown event IDs and delegates to the generator without changing any data. ([#30](https://github.com/chylsy/seng300-group-project/issues/30))
- Added the "Get scheduling suggestions" option to the draft events menu in the console UI. ([#30](https://github.com/chylsy/seng300-group-project/issues/30))
- Added `Event.schedule(roomId, slot)`, which moves a DRAFT to SCHEDULED. ([#30](https://github.com/chylsy/seng300-group-project/issues/30))

**FR-05: Confirm and cancel** (Mckenzie)
- Added `EventPlanningService.confirm(eventId, roomId, slot)`, which first checks that the event is a DRAFT, 
- the room exists and is free, the time fits the event, and enough invitees are available, and only then moves the event to SCHEDULED. ([#8](https://github.com/chylsy/seng300-group-project/issues/8), [PR #36](https://github.com/chylsy/seng300-group-project/pull/36))
- Added `EventPlanningService.cancel(eventId, confirmed)`, which only works on a SCHEDULED event, needs the user to confirm, and then sets the status to CANCELLED and frees the room. ([#8](https://github.com/chylsy/seng300-group-project/issues/8), [PR #36](https://github.com/chylsy/seng300-group-project/pull/36))
- Added "Confirm event" and "Cancel event" to the draft events menu in the console UI (options 6 and 7). ([#8](https://github.com/chylsy/seng300-group-project/issues/8))

**FR-06: Attendance responses** (Sara)
- TODO: summary and issue link.

**FR-07: Event list and roster CSV export** (Favour)
- TODO: summary and issue link.

**FR-08: Undo and redo** (Parth)
- TODO: summary and issue link.

**FR-04: Eligibility rule** (Nadine)
- TODO: name of the class implementing `EligibilityRule`, plus issue link.

### Design changes

- Added the `EligibilityRule` interface (`ineligibleReason(Event, String, TimeInterval)`), so suggestion logic does not depend on the concrete rule and can be tested with a stub. 
- Added the planned P2 classes `Suggestion`, `SuggestionResult`, `SuggestionGenerator` and `SchedulingService` to the class diagram. `EligibilityRule` and its implementation were not in the P1 diagram.
- TODO (Parth): Command pattern classes for undo/redo.
- TODO (All): any changes made to UMl, add them here

### Resolved defects

- TODO: list your defects here
- Corrected `traceability.csv` rows that pointed to tests that did not exist, and updated the issue numbers for the Room and FR-04 rows.
