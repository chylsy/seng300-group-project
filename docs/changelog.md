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
- TODO: summary and issue link.

**FR-04: Eligibility rule** (Nadine)
- Added `AvailabilityEligibilityRule`, an implementation of `EligibilityRule` used by `SuggestionGenerator`. A student is eligible for a slot when their saved availability covers the whole slot and they have no ACCEPTED attendance at another SCHEDULED event that overlaps it. `ineligibleReason(event, studentId, slot)` returns `null` when the student can attend; otherwise it returns "not available at that time" or "already accepted another event at that time", and the availability check runs first. ([#36](https://github.com/chylsy/seng300-group-project/commit/253f0b43cea39c9ad7791e40518f9d0f9f57ac66))
- Intervals are half-open, so back-to-back events don't conflict. PENDING and DECLINED responses, cancelled events, events the student isn't invited to, and the event being planned itself don't block the student. Checking eligibility never changes any data. ([#36](https://github.com/chylsy/seng300-group-project/commit/253f0b43cea39c9ad7791e40518f9d0f9f57ac66))
- Added `AvailabilityEligibilityRuleTest`, with 18 unit tests covering AC-04-13 to AC-04-22, the reason order when both checks fail (AC-04-12), and the five-of-six-eligible case run through `SuggestionGenerator` (AC-04-01). ([#36]())
- Added the matching FR-04 rows to `traceability.csv`. ([#42](https://github.com/chylsy/seng300-group-project/commit/4348dd5095dd7cd53db8c3327f24ede24ad470d4))



### Design changes

- Added the `EligibilityRule` interface (`ineligibleReason(Event, String, TimeInterval)`), so suggestion logic does not depend on the concrete rule and can be tested with a stub. 
- Added the planned P2 classes `Suggestion`, `SuggestionResult`, `SuggestionGenerator` and `SchedulingService` to the class diagram. `EligibilityRule` and its implementation were not in the P1 diagram.
- TODO (Parth): Command pattern classes for undo/redo.
- TODO (All): any changes made to UMl, add them here

### Resolved defects

- TODO: list your defects here
- Corrected `traceability.csv` rows that pointed to tests that did not exist, and updated the issue numbers for the Room and FR-04 rows.
- Added FR-01,FR-02,FR-03 to requirements.pdf which were missing from P1
