# YEBU Tour - Reservation System

JavaFX desktop application for hotel reservation management.

## How to run

```bash
cd "/Users/efeinan/Downloads/project for se"
chmod +x run-app.sh
./run-app.sh
```

## Default admin account

- Email: `admin@hotel.com`
- Password: `admin123`

## Folder layout

```
src/com/hotel/       # Java sources (ui, service, model, database, MainApp)
resources/com/hotel/
  fxml/              # Screens
  css/               # Stylesheet
  images/            # Logo and assets
docs/                # Project documentation
data/                # Runtime CSV data (created when the app runs)
```

## Architecture

- `com.hotel.ui` — Controllers / UI layer  
- `com.hotel.service` — Business logic  
- `com.hotel.database.dao` — Data access  
- `com.hotel.database` — Persistence bootstrap  
- `com.hotel.model` — Domain models  

## Persistent data

Data survives after the app closes. CSV files:

- `data/users.csv`
- `data/rooms.csv`
- `data/reservations.csv`

## Documentation

See the `docs/` folder:

- `docs/requirements.md`
- `docs/design.md`
- `docs/uml.md`
- `docs/test-document.md`
- `docs/final-report-outline.md`
