package org.example.cli;

public class ConsoleUI {
    private static final String REQUEST_PRICE_ZONE_PREFIX = "Elområde: ";

    private static final String REQUEST_PRICE_ZONE_MSG = "Ange ett elområde: SE1, SE2, SE3, eller SE4";
    private static final String INVALID_PRICE_ZONE_MSG = "Ogiltigt elområde, ange: SE1, SE2, SE3, eller SE4";

    public void printCommandMenu() {
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

    public String choosePriceZone() {
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
}
