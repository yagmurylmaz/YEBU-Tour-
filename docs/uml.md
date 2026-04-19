# UML Diagrams

## Use Case (Textual)

- Customer:
  - Login
  - Search room
  - Make reservation
  - View/cancel own reservations
- Admin:
  - Login
  - Manage rooms (CRUD)
  - Approve/cancel reservations
  - View reports

## Class Diagram (Mermaid)

```mermaid
classDiagram
direction TB

class User
class Customer
class Room
class Reservation
class Service
class BreakfastService
class GymService
class PoolService
class ParkingService

User <|-- Customer
Service <|-- BreakfastService
Service <|-- GymService
Service <|-- PoolService
Service <|-- ParkingService

class IReservationService
class ReservationService
IReservationService <|.. ReservationService

class IUserDAO
class UserDAO
class IRoomDAO
class RoomDAO
class IReservationDAO
class ReservationDAO

IUserDAO <|.. UserDAO
IRoomDAO <|.. RoomDAO
IReservationDAO <|.. ReservationDAO
```

## Activity (Reservation Flow)

```mermaid
flowchart TD
    A[Start] --> B[Customer searches rooms]
    B --> C{Valid date range?}
    C -- No --> D[Show validation error]
    D --> B
    C -- Yes --> E[Show available rooms]
    E --> F[Customer selects room]
    F --> G[Optional services selected]
    G --> H[Total price calculated]
    H --> I[Create reservation]
    I --> J[Persist to storage]
    J --> K[Show success message]
    K --> L[End]
```
