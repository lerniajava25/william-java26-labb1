package org.example;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    static final String BASE_API = "https://www.elprisetjustnu.se/api/v1/prices/";

    void main() {
        IO.println("Hello World!");
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
