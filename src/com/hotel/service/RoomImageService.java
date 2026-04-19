package com.hotel.service;

import com.hotel.database.dao.RoomDAO;
import com.hotel.database.dao.RoomImageDAO;
import com.hotel.model.Room;
import com.hotel.model.RoomImage;

import java.util.List;

public class RoomImageService {
    private final RoomImageDAO roomImageDAO = new RoomImageDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public List<RoomImage> getImagesForRoom(int roomId) {
        return roomImageDAO.findByRoomId(roomId);
    }

    /**
     * Adds an image and keeps rooms.image_path as a cover photo if missing.
     */
    public int addImage(int roomId, String absolutePath) {
        int sort = roomImageDAO.getNextSortOrder(roomId);
        RoomImage img = new RoomImage(roomId, absolutePath, sort);
        int id = roomImageDAO.add(img);

        // keep a cover photo for existing UI / queries
        Room room = roomDAO.findById(roomId).orElse(null);
        if (room != null && (room.getImagePath() == null || room.getImagePath().isBlank())) {
            room.setImagePath(absolutePath);
            roomDAO.update(room);
        }
        return id;
    }

    public boolean deleteImage(int imageId) {
        return roomImageDAO.deleteById(imageId);
    }
}

