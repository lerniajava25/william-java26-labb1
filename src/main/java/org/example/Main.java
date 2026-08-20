package org.example;

import java.util.Objects;

public class Main {
    void main() {
        IO.print("""
                Elpriser – Analysverktyg
                ========================
                1. Välj elområde (SE1, SE2, SE3, SE4)
                2. Min, Max och Medelpris
                3. Sortera priser (lägst till högst)
                4. Bästa laddningstid (4h sammanhängande)
                e. Avsluta
                """);
        String command;
        do {
            command = IO.readln("Kommando: ");
            switch (command) {
                case "1":
                    break;
                default:
                    break;
            }
        } while (!isExitCommand(command));
    }

    boolean isExitCommand(String command) {
        return Objects.equals(command, "e") || Objects.equals(command, "E");
    }
}
