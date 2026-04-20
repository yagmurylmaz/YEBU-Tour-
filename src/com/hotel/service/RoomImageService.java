package com.hotel.service;

import com.hotel.database.dao.RoomDAO;
import com.hotel.database.dao.RoomImageDAO;
import com.hotel.model.Room;
import com.hotel.model.RoomImage;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RoomImageService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final Map<Integer, CacheEntry> ROOM_IMAGE_CACHE = new ConcurrentHashMap<>();

    private final RoomImageDAO roomImageDAO = new RoomImageDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public List<RoomImage> getImagesForRoom(int roomId) {
        CacheEntry cached = ROOM_IMAGE_CACHE.get(roomId);
        if (cached != null && Instant.now().isBefore(cached.cachedAt.plus(CACHE_TTL))) {
            return cached.images;
        }
        List<RoomImage> fresh = roomImageDAO.findByRoomId(roomId);
        ROOM_IMAGE_CACHE.put(roomId, new CacheEntry(fresh));
        return fresh;
    }

    /**
     * Adds an image and keeps rooms.image_path as a cover photo if missing.
     */
    public int addImage(int roomId, String absolutePath) {
        int sort = roomImageDAO.getNextSortOrder(roomId);
        RoomImage img = new RoomImage(roomId, absolutePath, sort);
        int id = roomImageDAO.add(img);
        ROOM_IMAGE_CACHE.remove(roomId);

        // keep a cover photo for existing UI / queries
        Room room = roomDAO.findById(roomId).orElse(null);
        if (room != null && (room.getImagePath() == null || room.getImagePath().isBlank())) {
            room.setImagePath(absolutePath);
            roomDAO.update(room);
        }
        return id;
    }

    public boolean deleteImage(int imageId) {
        boolean ok = roomImageDAO.deleteById(imageId);
        if (ok) ROOM_IMAGE_CACHE.clear();
        return ok;
    }

    private static final class CacheEntry {
        private final List<RoomImage> images;
        private final Instant cachedAt;

        private CacheEntry(List<RoomImage> images) {
            this.images = List.copyOf(images);
            this.cachedAt = Instant.now();
        }
    }
}

