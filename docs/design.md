# Design Document

## System Architecture

Layered structure:

1. UI Layer (`com.hotel.ui`)
2. Business Layer (`com.hotel.service`)
3. Data Access Layer (`com.hotel.database.dao`)
4. Persistence Layer (`com.hotel.database`)
5. Model Layer (`com.hotel.model`)

## Class Design (summary)

- `User`, `Customer`, `Hotel`, `Room`, `Reservation`, `HotelReview`, `Service` (+ concrete service types)  
- `AuthService`, `HotelService`, `RoomService`, `ReservationService`, `ReportService`, `FavoriteHotelService`, `HotelReviewService`  
- `UserDAO`, `HotelDAO`, `RoomDAO`, `ReservationDAO`, `FavoriteHotelDAO`, `HotelReviewDAO`  
- `DatabaseConnection`, `DatabaseInitializer`  

## Data Design

MySQL tables (core):

- `users` — id, full_name, email, password_hash, phone, role  
- `hotels` — id, name, city_id, country_id, image_path, description  
- `rooms` — id, hotel_id, room_no, room_type, price_per_night, capacity, description, available  
- `reservations` — id, customer_id, room_id, check_in_date, check_out_date, total_price, status, created_at
- `hotel_reviews` — id, hotel_id, user_id, stars, comment, created_at
- `favorite_hotels` — user_id, hotel_id, created_at

## Interface Design

Main screens:

- Login  
- Register  
- Customer dashboard  
- Room search  
- Reservation confirmation  
- My reservations  
- Forgot password / Reset password
- Admin dashboard  
- Admin rooms  
- Admin reports  

## Navigation

- Login → Admin dashboard / Customer dashboard  
- Customer dashboard → Room search / My reservations  
- Admin dashboard → Admin rooms / Admin reports  
- Login/Register/Reset screens share the same session-backed theme toggle (dark/light mode).
