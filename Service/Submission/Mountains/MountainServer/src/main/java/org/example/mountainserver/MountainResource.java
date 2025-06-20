package org.example.mountainserver;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * REST controller for managing mountains.
 * Provides endpoints for adding, getting, updating and deleting mountains.
 * @author 2014459
 * @version 1.0
 */
@RestController
public class MountainResource {

    private static final int FIRST_ID = 1;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final ArrayList<Mountain> mountainList = new ArrayList<>();
    private int nextId = FIRST_ID;

    /**
     * Adds a list of mountains.
     *
     * @param mountains The list of mountains to add.
     * @return A ResponseEntity with the appropriate status code.
     */
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addMountains(@RequestBody final List<Mountain> mountains) {

        try {
            writeLock.lock();
            if (mountains.isEmpty()) {
                return ResponseEntity.badRequest().build();
            } else if (!Collections.disjoint(mountainList, mountains)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else {
                for (Mountain mountain : mountains) {
                    boolean mountainExists = mountainList.stream()
                            .anyMatch(m -> m.getName().equals(mountain.getName())
                                    && m.getAltitude() == mountain.getAltitude()
                                    && m.getRange().equals(mountain.getRange())
                                    && m.getCountry().equals(mountain.getCountry())
                                    && m.getIsNorthern() == mountain.getIsNorthern());

                    if (!mountainExists) {
                        mountain.setId(nextId++);
                        mountainList.add(mountain);
                    }
                }
                return ResponseEntity.ok().build();
            }

        } finally {
            writeLock.unlock();
        }
    }


    /**
     * Gets mountains based on the given criteria.
     *
     * @param country The mountain's country.
     * @param range The mountain's range.
     * @param name The mountain's name.
     * @param id The mountain's id.
     * @param hemisphere The hemisphere filter.
     * @param altitude The altitude filter.
     * @return A ResponseEntity with the list of filtered mountains
     * or an appropriate status code.
     */
    @GetMapping(value = {"/", "country/{country}",
            "country/{country}/range/{range}",
            "country/{country}/range/{range}/name/{name}", "id/{id}"
    },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Mountain>> getMountains(
            @PathVariable(name = "country", required = false) final String country,
            @PathVariable(name = "range", required = false) final String range,
            @PathVariable(name = "name", required = false) final String name,
            @PathVariable(name = "id", required = false) final String id,
            @RequestParam(name = "northern-hemisphere", required = false) final String hemisphere,
            @RequestParam(name = "altitude", required = false) String altitude) {

        if (notValidMountain(country, range, name, hemisphere, altitude)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        try {
            readLock.lock();
            final List<Mountain> filteredList = filterList(country, range, name, id, hemisphere, altitude);
            if (filteredList.isEmpty()) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.ok(new ArrayList<>(filteredList));
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Updates an existing mountain based on the ID.
     *
     * @param id the mountain's identifier.
     * @param newMountain the mountain object with updated variables.
     * @return A ResponseEntity with the appropriate status code.
     */
    @PutMapping(value = "update-mountain/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateMountain(@PathVariable("id") int id,
                                               @RequestBody Mountain newMountain) {

        if (notValidMountain(newMountain.getCountry(), newMountain.getRange(), newMountain.getName(),
                String.valueOf(newMountain.getIsNorthern()), String.valueOf(newMountain.getAltitude()))) {
            return ResponseEntity.badRequest().build();
        }

        try {
            writeLock.lock();
            Optional<Mountain> existingMountain = mountainList.stream()
                    .filter(mountain -> mountain.getId() == id)
                    .findFirst();

            if (existingMountain.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                Mountain mountain = existingMountain.get();
                mountain.setName(newMountain.getName());
                mountain.setAltitude(newMountain.getAltitude());
                mountain.setRange(newMountain.getRange());
                mountain.setCountry(newMountain.getCountry());
                mountain.setIsNorthern(newMountain.getIsNorthern());

                return ResponseEntity.ok().build();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Deletes a mountain from their ID.
     *
     * @param id the mountain's ID.
     * @return A ResponseEntity with the appropriate status code.
     */
    @DeleteMapping(value = "delete-mountain/{id}")
    public ResponseEntity<Void> deleteMountain(@PathVariable("id") int id) {
        try {
            writeLock.lock();
            Optional<Mountain> mountainToDelete = mountainList.stream()
                    .filter(mountain -> mountain.getId() == id)
                    .findFirst();
            if (mountainToDelete.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                mountainList.remove(mountainToDelete.get());
                return ResponseEntity.ok().build();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Filters the list of mountains based on criteria.
     *
     * @param mountainCountry The country filter.
     * @param mountainRange The range filter.
     * @param mountainName The name filter.
     * @param id The ID filter.
     * @param hemisphere The hemisphere filter.
     * @param altitude The altitude filter.
     * @return A list of filtered mountains.
     */
    private List<Mountain> filterList(final String mountainCountry, final String mountainRange,
                                      final String mountainName, final String id, final String hemisphere,
                                      final String altitude) {
        return mountainList.stream()
                .filter(mountain -> mountainCountry == null || mountain.getCountry().equals(mountainCountry))
                .filter(mountain -> mountainRange == null || mountain.getRange().equals(mountainRange))
                .filter(mountain -> mountainName == null || mountain.getName().equals(mountainName))
                .filter(mountain -> id == null || mountain.getId() == Integer.parseInt(id))
                .filter(mountain -> hemisphere == null || String.valueOf(mountain.getIsNorthern()).equals(hemisphere))
                .filter(mountain -> altitude == null || mountain.getAltitude() > Integer.parseInt(altitude))
                .toList();
    }

    /**
     * Checking if a mountain is valid by name, and matching a country to
     * ones provided.
     *
     * @param country The mountain's country.
     * @param name The mountain's name.
     * @return True if the mountain is not valid, false otherwise.
     */
    private boolean notValidMountain(String country, String range, String name, String hemisphere, String altitude) {
        boolean validCountry = country == null || isValidCountry(country);
        boolean validRange = range == null || isValidRange(range);
        boolean validName = name == null || isValidName(name);
        boolean validHemisphere = hemisphere == null || isValidHemisphere(hemisphere);
        boolean validAltitude = altitude == null || isValidAltitude(altitude);
        return !validCountry || !validRange || !validName || !validHemisphere || !validAltitude;
    }

    /**
     * Check if the country is valid from an array of countries.
     *
     * @param country The country to check.
     * @return True if the country is valid, false otherwise.
     */
    private boolean isValidCountry(String country) {
        //ideally all countries in the world should be added or stored elsewhere
        List<String> validCountries = Arrays.asList("Argentina", "Nepal", "Peru", "Wales", "Cymru");
        return validCountries.contains(country);
    }

    /**
     * Check if the mountain's range is valid.
     *
     * @param range The mountain's range.
     * @return True is the range is valid, false otherwise.
     */
    private boolean isValidRange(String range) {
        List<String> validRanges = Arrays.asList("Eryri", "Snowdonia", "Andes", //more ranges should be added
                "Himalayas", "BannauBrycheiniog", "Annapurna");
        return validRanges.contains(range);
    }

    /**
     * Check if the name of a mountain is valid.
     *
     * @param name The name to check.
     * @return True if the name is valid, false otherwise.
     */
    private boolean isValidName(String name) {
        return name != null && !name.isEmpty();
    }

    /**
     * Check if the hemisphere is valid.
     *
     * @param hemisphere The mountain's hemisphere.
     * @return True if the mountain's hemisphere is valid, false otherwise.
     */
    private boolean isValidHemisphere(String hemisphere) {
        return "true".equals(hemisphere) || "false".equals(hemisphere);
    }

    /**
     * Check if the altitude is valid.
     *
     * @param altitude The mountain's altitude.
     * @return True if the altitude is valid, false otherwise.
     */
    private boolean isValidAltitude(String altitude) {
        try {
            int altitudeVal = Integer.parseInt(altitude);
            return altitudeVal > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}



