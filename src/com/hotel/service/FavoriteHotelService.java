package com.hotel.service;

import com.hotel.database.dao.FavoriteHotelDAO;
import com.hotel.model.Hotel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FavoriteHotelService {
    private final FavoriteHotelDAO favoriteHotelDAO = new FavoriteHotelDAO();

    public Set<Integer> getFavoriteHotelIdsForUser(int userId) {
        if (userId <= 0) return Set.of();
        return favoriteHotelDAO.findFavoriteHotelIdsByUser(userId);
    }

    public boolean toggleFavorite(int userId, int hotelId) {
        if (userId <= 0) throw new IllegalArgumentException("User not found.");
        if (hotelId <= 0) throw new IllegalArgumentException("Hotel not found.");
        Set<Integer> existing = favoriteHotelDAO.findFavoriteHotelIdsByUser(userId);
        if (existing.contains(hotelId)) {
            favoriteHotelDAO.removeFavorite(userId, hotelId);
            return false;
        }
        return favoriteHotelDAO.addFavorite(userId, hotelId);
    }

    public List<Hotel> filterFavoriteHotels(List<Hotel> hotels, Set<Integer> favoriteHotelIds) {
        if (hotels == null || hotels.isEmpty()) return List.of();
        if (favoriteHotelIds == null || favoriteHotelIds.isEmpty()) return List.of();
        return hotels.stream()
            .filter(h -> favoriteHotelIds.contains(h.getId()))
            .collect(Collectors.toList());
    }
}
