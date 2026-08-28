package org.example;

import org.example.cli.ConsoleUI;
import org.example.model.TimeSlotPrice;
import org.example.service.PriceAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String REQUEST_COMMAND_PREFIX = "Kommando: ";

    private static final String INVALID_COMMAND_MSG = "Ange ett giltigt kommando!";

    private static final PriceAnalyzer priceAnalyzer = new PriceAnalyzer();
    private static final ConsoleUI ui = new ConsoleUI();

    void main() {
        ui.printCommandMenu();
        String command;
        List<TimeSlotPrice> prices = new ArrayList<>();
        while ((command = IO.readln(REQUEST_COMMAND_PREFIX)) != null) {
            switch (command) {
                case "1" -> {
                    String priceZone = ui.choosePriceZone();
                    if (priceZone != null) {
                        var newPrices = priceAnalyzer.loadPrices(priceZone);
                        if (!newPrices.isEmpty()) {
                            prices = newPrices;
                        }
                    }
                }
                case "2" -> priceAnalyzer.calculateMinMaxMean(prices);
                case "3" -> priceAnalyzer.sortPriceList(prices);
                case "4" -> priceAnalyzer.calculateOptimalChargingWindow(prices);
                case "e", "E" -> System.exit(0);
                default -> IO.println(INVALID_COMMAND_MSG);
            }
        }
    }
}
