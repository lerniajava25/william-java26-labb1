package org.example;

import java.util.Objects;

public class Main {
    void main() {
        String command;
        do {
            printCommandMenu();
            command = IO.readln("Kommando: ");
            switch (command) {
                case "1":
                    IO.println("Här ska elområdet väljas.");
                    break;
                case "2":
                    IO.println("Här ska min, max och medelpris beräknas och visas.");
                    break;
                case "3":
                    IO.println("Här ska priser sorteras från lägst till högst.");
                    break;
                case "4":
                    IO.println("Här ska bästa laddningstid beräknas och visas.");
                    break;
                case "e":
                    break;
                default:
                    IO.println("Ange ett giltigt kommando!");
                    break;
            }
        } while (!isExitCommand(command));
    }

    void printCommandMenu() {
        IO.print("""
                Elpriser – Analysverktyg
                ========================
                1. Välj elområde (SE1, SE2, SE3, SE4)
                2. Min, Max och Medelpris
                3. Sortera priser (lägst till högst)
                4. Bästa laddningstid (4h sammanhängande)
                e. Avsluta
                """);
    }

    boolean isExitCommand(String command) {
        return Objects.equals(command, "e") || Objects.equals(command, "E");
    }
}
