# CampusPlan (SENG 300 Group Project)

Official group project repository for SENG 300 – Introduction to Software Engineering, Fall 2026, University of Calgary.

## Project Overview

CampusPlan is a local Java command-line application that helps students organize study sessions, project meetings, club workshops and other group activities. Organizers can manage students, track availability, find suitable rooms, create events and generate scheduling suggestions.

## Team

| Name | UCID | Email |
| :--- | :--- | :--- |
| Sara Affara | 30241537 | sara.affara1@ucalgary.ca |
| Favour Iheanyichukwu | 30245516 | favour.iheanyichukwu@ucalgary.ca |
| Mawunyo Lotsugah | 30242155 | lotsugah.mawunyo@ucalgary.ca |
| Chylsy Marable | 30241044 | chylsy.marable@ucalgary.ca |
| Ramsha Oad | 30275772 | ramsha.oad@ucalgary.ca |
| Parth Singh | 30250082 | parth.singh1@ucalgary.ca |
| Nadine Torres | 30229594 | nadine.torres@ucalgary.ca |

## Prerequisites

- JDK 21 or newer
- Git

Maven does not need to be installed. The project includes the Maven Wrapper (`mvnw`).

## Build and Test

From the project root:

```bash
./mvnw clean test
```

On Windows, use `mvnw.cmd clean test`.

## Running the Application

```bash
./mvnw -q compile exec:java
```

On Windows, use `mvnw.cmd -q compile exec:java`.

Run commands from the project root so the app can find `data/` and `sample-data/`.

## Data File

- Application data is saved to `data/campusplan.json` (created on first save).
- Data is saved when you choose **5. Save data** or answer **y** to "Save before exiting?".
- Saving writes to a temporary file first, so a failed save keeps the last valid file.
- If the data file is corrupt, the app shows an error and exits without overwriting it.
- `data/` is excluded from Git.

## Seed Data

If no saved data exists at startup, the app asks:

```
Load sample data from sample-data/seed.json? (y/n)
```

Answer **y** to load the sample students, rooms, availability and events. Seed data can only be loaded into an empty application.

`sample-data/no-suggestions.json` is an additional dataset for testing the no-suggestion case.

## Reset and Backup

- **Back up:** copy `data/campusplan.json` somewhere safe.
- **Restore:** copy the backup back to `data/campusplan.json`.
- **Reset to empty:** delete `data/campusplan.json`. The next start offers the seed data again.

## Project Structure

```text
src/main/java/campusplan/
├── Main/          Command-line interface (entry point: Main)
├── service/       Application services (students, rooms, availability, events, storage)
├── domain/        Domain model and business rules
├── persistence/   DataStore interface, JSON and in-memory implementations
└── error/         CampusPlanException and its subclasses
src/test/java/campusplan/   JUnit 5 tests
sample-data/                Seed and test datasets
docs/                       Requirements, design, plan, traceability, test reports, release info
```

Dependencies point downward: the CLI calls services, services use the domain model, and persistence depends only on the domain.

## Development Workflow

- `main` is protected: changes go through a pull request with at least one approving review.
- Each task gets its own branch off `main` (for example `fix/cli-startup`, `feature/confirm-cancel`).
- Every pull request includes a test log from `./mvnw clean test`.
- Releases are tagged `p1`, `p2` and `p3`.

## Known Limitations

- Scheduling suggestions (FR-04) are available from **4. Manage draft events → 5. Get scheduling suggestions**.
- Confirm/cancel (FR-05) is implemented in the service layer but does not have a CLI menu option yet.
- Attendance responses (FR-06), event listing and roster export (FR-07) and undo/redo (FR-08) are in progress.
- Data is only saved when the user chooses to save.
- The command-line interface is English only.