# Design Document

## System Architecture

Layered structure:

1. UI Layer (`com.hotel.ui`)
2. Business Layer (`com.hotel.service`)
3. Data Access Layer (`com.hotel.database.dao`)
4. Persistence Layer (`com.hotel.database`)
5. Model Layer (`com.hotel.model`)

## Class Design (summary)

- `User`, `Customer`, `Room`, `Reservation`, `Service` (+ concrete service types)  
- `AuthService`, `RoomService`, `ReservationService`, `ReportService`  
- `UserDAO`, `RoomDAO`, `ReservationDAO`  
- `DatabaseConnection`, `DatabaseInitializer`  

## Data Design

CSV files:

- `users.csv` — id, fullName, email, passwordHash, phone, role  
- `rooms.csv` — id, roomNo, roomType, price, capacity, description, available  
- `reservations.csv` — id, customerId, roomId, checkIn, checkOut, total, status, createdAt  

## Interface Design

Main screens:

- Login  
- Register  
- Customer dashboard  
- Room search  
- Reservation confirmation  
- My reservations  
- Admin dashboard  
- Admin rooms  
- Admin reports  

## Navigation

- Login → Admin dashboard / Customer dashboard  
- Customer dashboard → Room search / My reservations  
- Admin dashboard → Admin rooms / Admin reports  
