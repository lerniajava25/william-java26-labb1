package org.example.client;

import org.example.model.TimeSlotPrice;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ElectricityPriceClient {
    private static final String BASE_API = "https://www.elprisetjustnu.se/api/v1/prices/";

    private static final HttpClient client = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_3)
            .build();

    public List<TimeSlotPrice> fetchPrices(String priceZone) throws IOException, InterruptedException {
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
