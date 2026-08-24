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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class Main {
    static final String BASE_API = "https://www.elprisetjustnu.se/api/v1/prices/";

    HttpClient httpClient = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_3)
            .build();

    void main() {
        printCommandMenu();
        String command;
        var prices = new TimeSlotPrice[0];
        while ((command = IO.readln("Kommando: ")) != null) {
            if (isExitCommand(command)) {
                break;
            }

            if (Objects.equals(command, "1")) {
                var newPrices = choosePriceZone();
                if (newPrices.length > 0) {
                    prices = newPrices;
                }
            } else if (Objects.equals(command, "2")) {
                calculateMinMaxMean(prices);
            } else if (Objects.equals(command, "3")) {
                sortPriceList(prices);
            } else {
                IO.println("Ange ett giltigt kommando!");
            }
        }
    }

    void printCommandMenu() {
        IO.print("""
                Elpriser – Analysverktyg
                ========================
                1. Välj elområde (SE1, SE2, SE3, SE4)
                2. Min, Max och Medelpris
                3. Sortera priser (lägst till högst)
                e. Avsluta
                """);
    }

    boolean isExitCommand(String command) {
        return Objects.equals(command, "e") || Objects.equals(command, "E");
    }

    TimeSlotPrice[] choosePriceZone() {
        try {
            String priceZone;
            IO.println("Ange ett elområde: SE1, SE2, SE3, eller SE4");
            while ((priceZone = IO.readln("Elområde: ")) != null) {
                if (!isValidPriceZone(priceZone)) {
                    IO.println("Ogiltigt elområde, ange: SE1, SE2, SE3, eller SE4");
                    continue;
                }
                IO.println("Du har valt elområde: " + priceZone);
                return fetchPrices(httpClient, priceZone);
            }
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

    void calculateMinMaxMean(TimeSlotPrice[] prices) {
        if (prices.length == 0) {
            IO.println("Välj elområde först!");
            return;
        }

        double minPrice = prices[0].SEK_per_kWh();
        double maxPrice = prices[0].SEK_per_kWh();
        double meanPrice = 0;
        for (TimeSlotPrice price : prices) {
            if (price.SEK_per_kWh() < minPrice)
                minPrice = price.SEK_per_kWh();
            if (price.SEK_per_kWh() > maxPrice)
                maxPrice = price.SEK_per_kWh();
            meanPrice += price.SEK_per_kWh();
        }
        meanPrice = meanPrice / prices.length;

        minPrice = Math.round(minPrice * 100);
        maxPrice = Math.round(maxPrice * 100);
        meanPrice = Math.round(meanPrice * 100);

        IO.println(String.format("Dagens lägsta elpris: %.0f öre/kWh", minPrice));
        IO.println(String.format("Dagens högsta elpris: %.0f öre/kWh", maxPrice));
        IO.println(String.format("Dagens medelpris: %.0f öre/kWh", meanPrice));
    }

    void sortPriceList(TimeSlotPrice[] prices) {
        if (prices.length == 0) {
            IO.println("Välj elområde först!");
            return;
        }

        TimeSlotPrice[] sorted = prices.clone();
        Arrays.sort(sorted, Comparator.comparing(TimeSlotPrice::SEK_per_kWh));
        for (TimeSlotPrice entry : sorted) {
            ZonedDateTime entryTimeStart = ZonedDateTime.parse(entry.time_start());
            ZonedDateTime entryTimeEnd = ZonedDateTime.parse(entry.time_end());
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");

            long roundedPrice = Math.round(entry.SEK_per_kWh() * 100);

            String priceString = "Elpris mellan kl %s - %s: %d öre/kWh";
            String formattedPriceString = String.format(priceString, entryTimeStart.format(dtf), entryTimeEnd.format(dtf), roundedPrice);
            IO.println(formattedPriceString);
        }
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
