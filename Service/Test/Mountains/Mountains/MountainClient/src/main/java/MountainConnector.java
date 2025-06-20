import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Mountain Client connector class for interacting with the Mountain server.
 * Provides methods for adding, getting, updating and deleting mountains.
 * @author 2014459
 * @version 1.0
 */
public class MountainConnector {

    private static final String SERVICE_URI = "http://localhost:8080/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Creates a new object of the MountainConnector.
     */
    public MountainConnector(String baseUri) {
    }

    /**
     * Adds a list of mountains to the service.
     *
     * @param mountains The list of mountains.
     * @return An optional containing the response from the server,
     * or empty if an error has occurred.
     */
    public Optional<Response> addMountains(final List<Mountain> mountains) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(SERVICE_URI))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(mountains)))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Response customResponse = new Response(Collections.emptyList(), response);
            return Optional.of(customResponse);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Gets all mountains from the server.
     *
     * @return An Optional with a list of all mountains,
     * or empty if an error occurs.
     */
    public Optional<Response> getAll() {
        return getMountains("");
    }

    /**
     * Get mountains by their country
     *
     * @param country The country parameter.
     * @return An optional containing all the countries' mountains,
     * or empty if an error occurred.
     */
    public Optional<Response> getByCountry(final String country) {
        String path = "country/" + country;
        return getMountains(path);
    }

    /**
     * Get mountains by their country and range
     *
     * @param country The country parameter.
     * @param range The range parameter.
     * @return A list of mountains from their country and range,
     * or empty if an error occurs.
     */
    public Optional<Response> getByCountryAndRange(final String country, final String range) {
        String path = "country/" + country + "/range/" + range;
        return getMountains(path);
    }

    /**
     * Gets mountains by their hemisphere
     *
     * @param isNorthern The hemisphere filter (true for northern, false for southern).
     * @return A list of mountains within the given hemisphere.
     * Or empty if there is an error.
     */
    public Optional<Response> getByHemisphere(boolean isNorthern) {
        String path = "?northern-hemisphere=" + isNorthern;
        return getMountains(path);
    }

    /**
     * Get mountains by their country and altitude.
     *
     * @param country The country parameter.
     * @param altitude The altitude parameter.
     * @return The list of mountains in a specified country and over a given altitude,
     * or empty if an error occurs.
     */
    public Optional<Response> getByCountryAltitude(String country, int altitude) {
        String path = "country/" + country + "?altitude=" + altitude;
        return getMountains(path);
    }

    /**
     * Get a mountain by their name.
     *
     * @param country The mountain's country parameter.
     * @param range The mountain's range parameter.
     * @param name The mountain's name parameter.
     * @return A mountain from the given name, or empty if an error occurs.
     */
    public Optional<Response> getByName(final String country, final String range, final String name) {
        String path = "country/" + country + "/range/" + range + "/name/" + name;
        return getMountains(path);
    }

    /**
     * Get a mountain by their ID.
     *
     * @param id The mountain's ID parameter.
     * @return The mountain from their ID, or empty if an error occurs.
     */
    public Optional<Response> getById(final int id) {
        String path = "id/" + id;
        return getMountains(path);
    }

    /**
     * Gets mountains based on the given path arguments.
     *
     * @param pathArgs The path arguments for the GET request.
     * @return The list of mountains matching the arguments,
     * or empty if an error occurs.
     */
    public Optional<Response> getMountains(final String pathArgs) {
        try {
            URI getUri = new URI(SERVICE_URI + pathArgs);
            HttpRequest request = HttpRequest.newBuilder().uri(getUri)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.body().isEmpty()) {
                return Optional.empty();
            } else {
                List<Mountain> mountains = objectMapper.readValue(response.body(), new TypeReference<>(){});
                return Optional.of(new Response(mountains, response));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Updates a mountain by its ID.
     *
     * @param id The ID of the mountain to update.
     * @param mountain The mountain object with the updated parameters.
     * @return An Optional with the server's response,
     * or an empty Optional if an error occurs.
     */
    public Optional<Response> updateMountain(int id, Mountain mountain) {
        try {
            String uri = SERVICE_URI + "update-mountain/" + id;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(mountain)))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(new Response(Collections.emptyList(), response));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Deletes a mountain by its ID.
     *
     * @param id The ID of the mountain to delete.
     * @return An Optional with the server's response,
     * or an empty Optional if an error occurs.
     */
    public Optional<Response> deleteMountain(int id) {
        try {
            String uri = SERVICE_URI + "delete-mountain/" + id;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(new Response(Collections.emptyList(), response));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
