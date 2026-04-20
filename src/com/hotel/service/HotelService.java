package com.hotel.service;

import com.hotel.database.dao.CityDAO;
import com.hotel.database.dao.CountryDAO;
import com.hotel.database.dao.HotelDAO;
import com.hotel.model.City;
import com.hotel.model.Country;
import com.hotel.model.Hotel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HotelService {
    private static final Duration COUNTRY_CACHE_TTL = Duration.ofHours(12);
    private static final Duration HOTEL_CACHE_TTL = Duration.ofMinutes(2);
    private static volatile List<String> countryCache = List.of();
    private static volatile Instant countryCacheAt = Instant.EPOCH;
    private static volatile List<Hotel> hotelCache = List.of();
    private static volatile Instant hotelCacheAt = Instant.EPOCH;

    private final CountryDAO countryDAO = new CountryDAO();
    private final CityDAO cityDAO = new CityDAO();
    private final HotelDAO hotelDAO = new HotelDAO();

    public List<String> getCountriesFromApi() {
        if (!countryCache.isEmpty() && Instant.now().isBefore(countryCacheAt.plus(COUNTRY_CACHE_TTL))) {
            return countryCache;
        }
        String url = "https://restcountries.com/v3.1/all?fields=name";
        HttpClient client = HttpClient.newHttpClient();
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
            try {
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() < 200 || res.statusCode() >= 300) {
                    throw new IllegalStateException("Country API returned status " + res.statusCode());
                }
                List<String> parsed = parseCountryNames(res.body());
                countryCache = parsed;
                countryCacheAt = Instant.now();
                return parsed;
            } catch (IOException | InterruptedException e) {
                last = e;
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            } catch (Exception e) {
                last = e;
            }
        }

        List<String> dbFallback = countryDAO.findAll().stream()
            .map(Country::getName)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        if (!dbFallback.isEmpty()) return dbFallback;
        throw new IllegalStateException("Could not fetch countries from API: " + (last != null ? last.getMessage() : "unknown error"), last);
    }

    public List<Hotel> getAllHotels() {
        if (!hotelCache.isEmpty() && Instant.now().isBefore(hotelCacheAt.plus(HOTEL_CACHE_TTL))) {
            return hotelCache;
        }
        List<Hotel> fresh = hotelDAO.findAll();
        hotelCache = fresh;
        hotelCacheAt = Instant.now();
        return fresh;
    }

    public int addHotel(Hotel hotel, String countryName, String cityName) {
        int countryId = resolveCountryId(countryName);
        int cityId = resolveCityId(countryId, cityName);
        hotel.setCountryId(countryId);
        hotel.setCityId(cityId);
        validateHotel(hotel);
        int id = hotelDAO.add(hotel);
        invalidateHotelCache();
        return id;
    }

    public boolean updateHotel(Hotel hotel, String countryName, String cityName) {
        if (hotel.getId() <= 0) throw new IllegalArgumentException("Select a hotel to update.");
        int countryId = resolveCountryId(countryName);
        int cityId = resolveCityId(countryId, cityName);
        hotel.setCountryId(countryId);
        hotel.setCityId(cityId);
        validateHotel(hotel);
        boolean ok = hotelDAO.update(hotel);
        if (ok) invalidateHotelCache();
        return ok;
    }

    public boolean deleteHotel(int id) {
        boolean ok = hotelDAO.delete(id);
        if (ok) invalidateHotelCache();
        return ok;
    }

    private static void validateHotel(Hotel h) {
        if (h.getName() == null || h.getName().isBlank()) throw new IllegalArgumentException("Hotel name cannot be empty.");
        if (h.getCountryId() <= 0) throw new IllegalArgumentException("Country must be selected.");
        if (h.getCityId() <= 0) throw new IllegalArgumentException("City must be selected.");
        if (h.getAddressLine() == null || h.getAddressLine().isBlank()) throw new IllegalArgumentException("Address cannot be empty.");
    }

    private int resolveCountryId(String countryName) {
        if (countryName == null || countryName.isBlank()) throw new IllegalArgumentException("Country must be selected.");
        String normalized = countryName.trim();
        return countryDAO.findByName(normalized)
            .map(Country::getId)
            .orElseGet(() -> countryDAO.add(new Country(normalized)));
    }

    private int resolveCityId(int countryId, String cityName) {
        if (cityName == null || cityName.isBlank()) throw new IllegalArgumentException("City cannot be empty.");
        String normalized = cityName.trim();
        return cityDAO.findByCountryIdAndName(countryId, normalized)
            .map(City::getId)
            .orElseGet(() -> cityDAO.add(new City(countryId, normalized)));
    }

    private static List<String> parseCountryNames(String json) {
        List<String> names = new ArrayList<>();
        Pattern p = Pattern.compile("\"common\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String name = m.group(1);
            if (name != null && !name.isBlank()) names.add(name);
        }
        names = names.stream().distinct().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        if (names.isEmpty()) throw new IllegalStateException("Country API returned no countries.");
        return names;
    }

    private static void invalidateHotelCache() {
        hotelCache = List.of();
        hotelCacheAt = Instant.EPOCH;
    }
}

