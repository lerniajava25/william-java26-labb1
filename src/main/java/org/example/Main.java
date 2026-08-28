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
import java.util.*;

public class Main {
    static final String BASE_API = "https://www.elprisetjustnu.se/api/v1/prices/";

    static final String REQUEST_COMMAND_PREFIX = "Kommando: ";
    static final String REQUEST_PRICE_ZONE_PREFIX = "Elområde: ";

    static final String INVALID_COMMAND_MSG = "Ange ett giltigt kommando!";
    static final String NO_PRICE_ZONE_MSG = "Välj elområde först!";
    static final String REQUEST_PRICE_ZONE_MSG = "Ange ett elområde: SE1, SE2, SE3, eller SE4";
    static final String INVALID_PRICE_ZONE_MSG = "Ogiltigt elområde, ange: SE1, SE2, SE3, eller SE4";
    static final String INCORRECT_PRICE_ZONE_DATASET_SIZE_MSG = "Valt elområde saknade information för 4 timmars tid!";

    HttpClient httpClient = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_3)
            .build();

    void main() {
        printCommandMenu();
        String command;
        List<TimeSlotPrice> prices = new ArrayList<>();
        while ((command = IO.readln(REQUEST_COMMAND_PREFIX)) != null) {
            switch (command) {
                case "1" -> {
                    String priceZone = choosePriceZone();
                    var newPrices = loadPrices(priceZone);
                    if (!newPrices.isEmpty()) {
                        prices = newPrices;
                    }
                }
                case "2" -> calculateMinMaxMean(prices);
                case "3" -> sortPriceList(prices);
                case "4" -> calculateOptimalChargingWindow(prices);
                case "e", "E" -> System.exit(0);
                default -> IO.println(INVALID_COMMAND_MSG);
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
                4. Bästa laddningstid (4h sammanhängande)
                e. Avsluta
                """);
    }

    String choosePriceZone() {
        String priceZone;
        IO.println(REQUEST_PRICE_ZONE_MSG);
        while ((priceZone = IO.readln(REQUEST_PRICE_ZONE_PREFIX)) != null) {
            priceZone = priceZone.toUpperCase();
            switch (priceZone) {
                case "SE1", "SE2", "SE3", "SE4" -> {
                    IO.println("Du har valt elområde: " + priceZone);
                    return priceZone;
                }
                default -> IO.println(INVALID_PRICE_ZONE_MSG);
            }
        }
        return null;
    }

    List<TimeSlotPrice> loadPrices(String priceZone) {
        if (priceZone == null) {
            return Collections.emptyList();
        }

        try {
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
        return Collections.emptyList();
    }

    void calculateMinMaxMean(List<TimeSlotPrice> prices) {
        if (prices.isEmpty()) {
            IO.println(NO_PRICE_ZONE_MSG);
            return;
        }

        double minPrice = prices.getFirst().SEK_per_kWh();
        double maxPrice = prices.getFirst().SEK_per_kWh();
        double meanPrice = 0;
        for (TimeSlotPrice price : prices) {
            if (price.SEK_per_kWh() < minPrice)
                minPrice = price.SEK_per_kWh();
            if (price.SEK_per_kWh() > maxPrice)
                maxPrice = price.SEK_per_kWh();
            meanPrice += price.SEK_per_kWh();
        }
        meanPrice = meanPrice / prices.size();

        minPrice = Math.round(minPrice * 100);
        maxPrice = Math.round(maxPrice * 100);
        meanPrice = Math.round(meanPrice * 100);

        IO.println(String.format("Dagens lägsta elpris: %.0f öre/kWh", minPrice));
        IO.println(String.format("Dagens högsta elpris: %.0f öre/kWh", maxPrice));
        IO.println(String.format("Dagens medelpris: %.0f öre/kWh", meanPrice));
    }

    void sortPriceList(List<TimeSlotPrice> prices) {
        if (prices.isEmpty()) {
            IO.println(NO_PRICE_ZONE_MSG);
            return;
        }

        List<TimeSlotPrice> sorted = new ArrayList<>(prices);
        sorted.sort(Comparator.comparing(TimeSlotPrice::SEK_per_kWh));
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

    void calculateOptimalChargingWindow(List<TimeSlotPrice> prices) {
        final int WINDOW_SIZE = 16;

        if (prices.isEmpty()) {
            IO.println(NO_PRICE_ZONE_MSG);
            return;
        } else if (prices.size() < WINDOW_SIZE) {
            IO.println(INCORRECT_PRICE_ZONE_DATASET_SIZE_MSG);
            return;
        }

        double currentPriceSum = 0;
        double lowestPriceSum = Double.MAX_VALUE;

        int leftPointer = 0;
        int rightPointer = 0;

        TimeSlotPrice lowestLeftEntry = prices.getFirst();
        TimeSlotPrice lowestRightEntry = prices.get(15);

        while (rightPointer < prices.size()) {
            currentPriceSum += prices.get(rightPointer).SEK_per_kWh();
            if (rightPointer - leftPointer + 1 == WINDOW_SIZE) {
                if (currentPriceSum < lowestPriceSum) {
                    lowestPriceSum = currentPriceSum;
                    lowestLeftEntry = prices.get(leftPointer);
                    lowestRightEntry = prices.get(rightPointer);
                }
                currentPriceSum -= prices.get(leftPointer).SEK_per_kWh();
                leftPointer++;
            }
            rightPointer++;
        }

        ZonedDateTime timeStart = ZonedDateTime.parse(lowestLeftEntry.time_start());
        ZonedDateTime timeEnd = ZonedDateTime.parse(lowestRightEntry.time_end());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");

        long averageWindowPrice = Math.round((lowestPriceSum / WINDOW_SIZE) * 100);

        String chargingWindowString = "Bästa laddningstid idag (%s): kl %s - %s, medelpris: %d öre/kWh";
        String formattedChargingWindowString = String.format(
                chargingWindowString,
                dateFormatter.format(timeStart),
                timeFormatter.format(timeStart),
                timeFormatter.format(timeEnd),
                averageWindowPrice
        );
        IO.println(formattedChargingWindowString);
    }

    List<TimeSlotPrice> fetchPrices(HttpClient client, String priceZone) throws IOException, InterruptedException {
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
            return Collections.emptyList();
        }

        ObjectMapper mapper = new ObjectMapper();
        var priceArray = mapper.readValue(response.body(), TimeSlotPrice[].class);
        return Collections.unmodifiableList(Arrays.asList(priceArray));
    }
}

record TimeSlotPrice(double SEK_per_kWh, double EUR_per_kWh, double EXR, String time_start, String time_end) {
}
