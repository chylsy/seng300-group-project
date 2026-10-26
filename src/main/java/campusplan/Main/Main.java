
package campusplan.Main;

import java.util.Scanner;


public class Main {
    private final Scanner scanner;

    public Main() {
        scanner = new Scanner(System.in);
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
        System.out.println("Student management");
        // TODO: Connect to student application operations.
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
