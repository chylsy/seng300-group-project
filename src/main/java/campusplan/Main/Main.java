package campusplan.Main;

import java.util.Scanner;
import campusplan.service.StudentService;
import campusplan.domain.Student;
import campusplan.domain.CampusPlanData;
import campusplan.domain.IdGenerator;
import campusplan.error.DuplicateEmailException;
import campusplan.error.NotFoundException;

public class Main {
    private final Scanner scanner;
    private final StudentService studentService;

    public Main(CampusPlanData data, IdGenerator ids) {
        scanner = new Scanner(System.in);
        studentService = new StudentService(data, ids);
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
        System.out.println("\n=== All Students ===");

        for (Student student : studentService.listAll()) {
            System.out.println(student);
        }
    }

    private void searchStudents() {
        System.out.print("Enter a name or email to search: ");
        String text = scanner.nextLine();

        System.out.println("\n=== Search Results ===");

        for (Student student : studentService.search(text)) {
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
            System.out.println(e.getMessage());
        }
    }

    private void viewRooms() {
        System.out.println("Room catalogue");
        // TODO: Display rooms supplied by the application.
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