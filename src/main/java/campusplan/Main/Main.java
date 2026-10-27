package campusplan.Main;

import java.util.Scanner;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import campusplan.service.StudentService;
import campusplan.service.RoomCatalogService;
import campusplan.service.AvailabilityService;

import campusplan.domain.Student;
import campusplan.domain.Room;
import campusplan.domain.RoomFeature;
import campusplan.domain.CampusPlanData;
import campusplan.domain.IdGenerator;
import campusplan.domain.TimeInterval;

import campusplan.error.DuplicateEmailException;
import campusplan.error.NotFoundException;

public class Main {
    private final Scanner scanner;
    private final StudentService studentService;
    private final RoomCatalogService roomCatalogService;
    private final AvailabilityService availabilityService;

    public Main(CampusPlanData data, IdGenerator ids) {
        scanner = new Scanner(System.in);
        studentService = new StudentService(data, ids);
        roomCatalogService = new RoomCatalogService(data);
        availabilityService = new AvailabilityService(data);
    }

    public void run() {
        boolean running = true;

        while (running) {
            displayMenu();

            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    manageStudents();
                    break;
                case "2":
                    viewRooms();
                    break;
                case "3":
                    manageAvailability();
                    break;
                case "4":
                    manageDraftEvents();
                    break;
                case "5":
                    saveData();
                    break;
                case "0":
                    running = false;
                    System.out.println("Exiting CampusPlan.");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n=== CampusPlan ===");
        System.out.println("1. Manage students");
        System.out.println("2. View rooms");
        System.out.println("3. Manage availability");
        System.out.println("4. Manage draft events");
        System.out.println("5. Save data");
        System.out.println("0. Exit");
    }

    // ==================== STUDENTS ====================

    private void manageStudents() {
        boolean managing = true;

        while (managing) {
            System.out.println("\n=== Student Management ===");
            System.out.println("1. Create student");
            System.out.println("2. View all students");
            System.out.println("3. Search students");
            System.out.println("4. Edit student");
            System.out.println("5. Find student by ID");
            System.out.println("0. Back to main menu");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    createStudent();
                    break;
                case "2":
                    viewStudents();
                    break;
                case "3":
                    searchStudents();
                    break;
                case "4":
                    editStudent();
                    break;
                case "5":
                    findStudent();
                    break;
                case "0":
                    managing = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void createStudent() {
        try {
            System.out.print("Enter student name: ");
            String name = scanner.nextLine();

            System.out.print("Enter student email: ");
            String email = scanner.nextLine();

            Student student = studentService.create(name, email);

            System.out.println("Student created successfully:");
            System.out.println(student);
        } catch (DuplicateEmailException | IllegalArgumentException e) {
            System.out.println("Could not create student: " + e.getMessage());
        }
    }

    private void viewStudents() {
        List<Student> students = studentService.listAll();

        System.out.println("\n=== All Students ===");

        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }

        for (Student student : students) {
            System.out.println(student);
        }
    }

    private void searchStudents() {
        System.out.print("Enter a name or email to search: ");
        String text = scanner.nextLine();

        List<Student> students = studentService.search(text);

        System.out.println("\n=== Search Results ===");

        if (students.isEmpty()) {
            System.out.println("No matching students found.");
            return;
        }

        for (Student student : students) {
            System.out.println(student);
        }
    }

    private void editStudent() {
        try {
            System.out.print("Enter student ID: ");
            String id = scanner.nextLine();

            System.out.print("Enter new name: ");
            String name = scanner.nextLine();

            System.out.print("Enter new email: ");
            String email = scanner.nextLine();

            Student student = studentService.edit(id, name, email);

            System.out.println("Student updated successfully:");
            System.out.println(student);
        } catch (DuplicateEmailException | NotFoundException | IllegalArgumentException e) {
            System.out.println("Could not edit student: " + e.getMessage());
        }
    }

    private void findStudent() {
        try {
            System.out.print("Enter student ID: ");
            String id = scanner.nextLine();

            Student student = studentService.get(id);
            System.out.println(student);
        } catch (NotFoundException e) {
            System.out.println("Could not find student: " + e.getMessage());
        }
    }

    // ==================== ROOMS ====================

    private void viewRooms() {
        boolean viewing = true;

        while (viewing) {
            System.out.println("\n=== Room Catalogue ===");
            System.out.println("1. View all rooms");
            System.out.println("2. Filter rooms by minimum capacity");
            System.out.println("3. Filter rooms by capacity and features");
            System.out.println("0. Back to main menu");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayRooms(roomCatalogService.listAll());
                    break;
                case "2":
                    filterRoomsByCapacity();
                    break;
                case "3":
                    filterRoomsByCapacityAndFeatures();
                    break;
                case "0":
                    viewing = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void filterRoomsByCapacity() {
        try {
            System.out.print("Enter minimum capacity: ");
            int minCapacity = Integer.parseInt(scanner.nextLine().trim());

            displayRooms(roomCatalogService.filter(minCapacity, Set.of()));
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid whole number.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid capacity: " + e.getMessage());
        }
    }

    private void filterRoomsByCapacityAndFeatures() {
        try {
            System.out.print("Enter minimum capacity: ");
            int minCapacity = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\nAvailable room features:");
            for (RoomFeature feature : RoomFeature.values()) {
                System.out.println("- " + feature);
            }

            System.out.println("Enter required features separated by commas.");
            System.out.println("Leave blank for no feature filter.");
            System.out.print("Features: ");

            String input = scanner.nextLine().trim();
            Set<RoomFeature> requiredFeatures = EnumSet.noneOf(RoomFeature.class);

            if (!input.isEmpty()) {
                for (String featureName : input.split(",")) {
                    requiredFeatures.add(
                            RoomFeature.valueOf(featureName.trim().toUpperCase())
                    );
                }
            }

            displayRooms(roomCatalogService.filter(minCapacity, requiredFeatures));
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid whole number for capacity.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private void displayRooms(List<Room> rooms) {
        System.out.println("\n=== Rooms ===");

        if (rooms.isEmpty()) {
            System.out.println("No rooms found matching your criteria.");
            return;
        }

        for (Room room : rooms) {
            System.out.println(room);
        }
    }

    // ==================== AVAILABILITY ====================

    private void manageAvailability() {
        boolean managing = true;

        while (managing) {
            System.out.println("\n=== Manage Availability ===");
            System.out.println("1. Add availability");
            System.out.println("2. Remove availability");
            System.out.println("3. Check availability for a time interval");
            System.out.println("0. Back to main menu");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addAvailability();
                    break;
                case "2":
                    removeAvailability();
                    break;
                case "3":
                    checkAvailability();
                    break;
                case "0":
                    managing = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void addAvailability() {
        try {
            System.out.print("Enter student ID: ");
            String studentId = scanner.nextLine().trim();

            studentService.get(studentId);
            TimeInterval interval = readTimeInterval();

            availabilityService.addAvailability(studentId, interval);

            System.out.println("Availability added successfully.");
        } catch (IllegalArgumentException | NotFoundException e) {
            System.out.println("Could not add availability: " + e.getMessage());
        }
    }

    private void removeAvailability() {
        try {
            System.out.print("Enter student ID: ");
            String studentId = scanner.nextLine().trim();

            studentService.get(studentId);
            TimeInterval interval = readTimeInterval();

            availabilityService.removeAvailability(studentId, interval);

            System.out.println("Availability removed successfully.");
        } catch (IllegalArgumentException | NotFoundException e) {
            System.out.println("Could not remove availability: " + e.getMessage());
        }
    }

    private void checkAvailability() {
        try {
            System.out.print("Enter student ID: ");
            String studentId = scanner.nextLine().trim();

            studentService.get(studentId);
            TimeInterval interval = readTimeInterval();

            boolean available =
                    availabilityService.isAvailable(studentId, interval);

            if (available) {
                System.out.println("The student is available for this interval.");
            } else {
                System.out.println("The student is not available for this interval.");
            }
        } catch (IllegalArgumentException | NotFoundException e) {
            System.out.println("Could not check availability: " + e.getMessage());
        }
    }

    private TimeInterval readTimeInterval() {
        try {
            System.out.print("Enter date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(scanner.nextLine().trim());

            System.out.print("Enter start time (HH:MM): ");
            LocalTime start = LocalTime.parse(scanner.nextLine().trim());

            System.out.print("Enter end time (HH:MM): ");
            LocalTime end = LocalTime.parse(scanner.nextLine().trim());

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException(
                        "End time must be after start time."
                );
            }

            return new TimeInterval(date, start, end);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date or time. Use YYYY-MM-DD for the date and HH:MM for times."
            );
        }
    }

    private void manageDraftEvents() {
        System.out.println("Manage draft events");
        // TODO: Connect to the relevant event service.
    }

    private void saveData() {
        System.out.println("Save data");
        // TODO: Connect to the application's storage operation.
    }
}
