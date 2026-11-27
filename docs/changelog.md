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
- TODO: summary and issue link.

**FR-06: Attendance responses** (Sara)
- TODO: summary and issue link.

**FR-07: Event list and roster CSV export** (Favour)
- TODO: summary and issue link.

**FR-08: Undo and redo** (Parth)
- Added `u` and `r` to undo and redo the last 20 successful changes in the current session. A new successful change clears the redo history, and a failed change preserves it. [#11](https://github.com/chylsy/seng300-group-project/issues/11)
- Added `CommandHistoryTest` for history capacity, failed changes, redo after undo, and restoring a room reservation and attendance responses after saving and restarting.
- Added `sample-data/p2-baseline.json` as the P2 sample dataset for migration testing.

**FR-04: Eligibility rule** (Nadine)
- TODO: name of the class implementing `EligibilityRule`, plus issue link.

### Design changes

- Added the `EligibilityRule` interface (`ineligibleReason(Event, String, TimeInterval)`), so suggestion logic does not depend on the concrete rule and can be tested with a stub. 
- Added the planned P2 classes `Suggestion`, `SuggestionResult`, `SuggestionGenerator` and `SchedulingService` to the class diagram. `EligibilityRule` and its implementation were not in the P1 diagram.
- Implemented FR-08 using the Command pattern. `CommandHistory` manages the undo and redo stacks, and `SnapshotCommand` implements `Command` by capturing the application state before and after each successful change.
- TODO (All): any changes made to UMl, add them here

### Resolved defects

- TODO: list your defects here
- Corrected `traceability.csv` rows that pointed to tests that did not exist, and updated the issue numbers for the Room and FR-04 rows.
- Fixed JSON loading so saved events retain their status, room, time slot, and attendance responses after restarting.
