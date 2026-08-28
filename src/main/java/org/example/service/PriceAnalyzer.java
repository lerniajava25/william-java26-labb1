package org.example.service;

import org.example.client.ElectricityPriceClient;
import org.example.model.TimeSlotPrice;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PriceAnalyzer {
    private static final String NO_PRICE_ZONE_MSG = "Välj elområde först!";
    private static final String INCORRECT_PRICE_ZONE_DATASET_SIZE_MSG = "Valt elområde saknade information för 4 timmars tid!";

    private final ElectricityPriceClient priceClient;

    public PriceAnalyzer() {
        this(new ElectricityPriceClient());
    }

    public PriceAnalyzer(ElectricityPriceClient priceClient) {
        this.priceClient = priceClient;
    }

    public List<TimeSlotPrice> loadPrices(String priceZone) {
        if (priceZone == null) {
            return Collections.emptyList();
        }

        try {
            return priceClient.fetchPrices(priceZone);
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

    public void calculateMinMaxMean(List<TimeSlotPrice> prices) {
        if (prices.isEmpty()) {
            IO.println(NO_PRICE_ZONE_MSG);
            return;
        }

        double minPrice = prices.getFirst().sekPerKwh();
        double maxPrice = prices.getFirst().sekPerKwh();
        double meanPrice = 0;
        for (TimeSlotPrice price : prices) {
            if (price.sekPerKwh() < minPrice)
                minPrice = price.sekPerKwh();
            if (price.sekPerKwh() > maxPrice)
                maxPrice = price.sekPerKwh();
            meanPrice += price.sekPerKwh();
        }
        meanPrice = meanPrice / prices.size();

        minPrice = Math.round(minPrice * 100);
        maxPrice = Math.round(maxPrice * 100);
        meanPrice = Math.round(meanPrice * 100);

        IO.println(String.format("Dagens lägsta elpris: %.0f öre/kWh", minPrice));
        IO.println(String.format("Dagens högsta elpris: %.0f öre/kWh", maxPrice));
        IO.println(String.format("Dagens medelpris: %.0f öre/kWh", meanPrice));
    }

    public void sortPriceList(List<TimeSlotPrice> prices) {
        if (prices.isEmpty()) {
            IO.println(NO_PRICE_ZONE_MSG);
            return;
        }

        List<TimeSlotPrice> sorted = new ArrayList<>(prices);
        sorted.sort(Comparator.comparing(TimeSlotPrice::sekPerKwh));
        for (TimeSlotPrice entry : sorted) {
            ZonedDateTime entryTimeStart = ZonedDateTime.parse(entry.timeStart());
            ZonedDateTime entryTimeEnd = ZonedDateTime.parse(entry.timeEnd());
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");

            long roundedPrice = Math.round(entry.sekPerKwh() * 100);

            String priceString = "Elpris mellan kl %s - %s: %d öre/kWh";
            String formattedPriceString = String.format(priceString, entryTimeStart.format(dtf), entryTimeEnd.format(dtf), roundedPrice);
            IO.println(formattedPriceString);
        }
    }

    public void calculateOptimalChargingWindow(List<TimeSlotPrice> prices) {
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
        TimeSlotPrice lowestRightEntry = prices.get(WINDOW_SIZE - 1);

        while (rightPointer < prices.size()) {
            currentPriceSum += prices.get(rightPointer).sekPerKwh();
            if (rightPointer - leftPointer + 1 == WINDOW_SIZE) {
                if (currentPriceSum < lowestPriceSum) {
                    lowestPriceSum = currentPriceSum;
                    lowestLeftEntry = prices.get(leftPointer);
                    lowestRightEntry = prices.get(rightPointer);
                }
                currentPriceSum -= prices.get(leftPointer).sekPerKwh();
                leftPointer++;
            }
            rightPointer++;
        }

        ZonedDateTime timeStart = ZonedDateTime.parse(lowestLeftEntry.timeStart());
        ZonedDateTime timeEnd = ZonedDateTime.parse(lowestRightEntry.timeEnd());
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
}
