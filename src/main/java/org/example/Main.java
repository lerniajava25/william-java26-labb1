package org.example;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Main {
    static final String BASE_API = "https://www.elprisetjustnu.se/api/v1/prices/";

    HttpClient httpClient = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_3)
            .build();

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

    TimeSlotPrice[] choosePriceZone() {
        try {
            String priceZone;
            do {
                IO.println("Ange ett giltigt elområde: SE1, SE2, SE3, eller SE4");
                priceZone = IO.readln("Elområde: ");
            } while (!isValidPriceZone(priceZone));
            IO.println("Du har valt elområde: " + priceZone);
            return fetchPrices(httpClient, priceZone);
        } catch (HttpTimeoutException _) {
            IO.println("Servern tog för lång tid att svara, försök igen!");
        } catch (ConnectException _) {
            IO.println("Kunde inte ansluta till servern. Kontrollera din nätverksanslutning!");
        } catch (IOException e) {
            IO.println("Elpriser kunde inte hämtas, försök igen: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IO.println("Din förfrågan till servern avbröts, försök igen: " + e.getMessage());
        }
        return new TimeSlotPrice[0];
    }

    boolean isValidPriceZone(String zone) {
        return Objects.equals(zone, "SE1") ||
                Objects.equals(zone, "SE2") ||
                Objects.equals(zone, "SE3") ||
                Objects.equals(zone, "SE4");
    }

    TimeSlotPrice[] fetchPrices(HttpClient client, String priceZone) throws IOException, InterruptedException {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM-dd");
        String formattedDate = date.format(formatter);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(BASE_API + formattedDate + "_" + priceZone + ".json"))
                .build();
        var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            IO.println("Problem med förfrågan till servern! (status " + response.statusCode() + ")");
            return new TimeSlotPrice[0];
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body(), TimeSlotPrice[].class);
    }
}

record TimeSlotPrice(double SEK_per_kWh, double EUR_per_kWh, double EXR, String time_start, String time_end) {
}
