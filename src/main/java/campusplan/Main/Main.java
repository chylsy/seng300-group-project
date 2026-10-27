package campusplan.Main;

import java.util.Scanner;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;

import campusplan.service.StudentService;
import campusplan.service.RoomCatalogService;
import campusplan.domain.Student;
import campusplan.domain.Room;
import campusplan.domain.RoomFeature;
import campusplan.domain.CampusPlanData;
import campusplan.domain.IdGenerator;
import campusplan.error.DuplicateEmailException;
import campusplan.error.NotFoundException;

public class Main {
    private final Scanner scanner;
    private final StudentService studentService;
    private final RoomCatalogService roomCatalogService;

    public Main(CampusPlanData data, IdGenerator ids) {
        scanner = new Scanner(System.in);
        studentService = new StudentService(data, ids);
        roomCatalogService = new RoomCatalogService(data);
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

    private void manageAvailability() {
        System.out.println("Manage availability");
        // TODO: Collect availability and call the relevant service.
    }

    private void manageDraftEvents() {
        System.out.println("Manage draft events");
        // TODO: Collect event details and call the relevant service.
    }

    private void saveData() {
        System.out.println("Save data");
        // TODO: Call the application's storage operation.
    }
}