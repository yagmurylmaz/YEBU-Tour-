# YEBU Tour - Reservation System

JavaFX desktop application for hotel reservation management.

## MySQL setup

1. Install and start **MySQL Server** (8.x).
2. Create an empty database:  
   `CREATE DATABASE yebu_hotel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
3. Connection defaults are in `resources/database.properties`. To override user, password, or URL without editing that file, copy `config/db.properties.example` to `config/db.properties` and set `db.url`, `db.user`, and **`db.password`** to your real MySQL password (do not leave the example placeholder). The app searches upward from the current working directory to find `config/db.properties`, so it still works if the IDE’s working directory is not the project root. Alternatively, set the environment variable **`YEBU_DB_PASSWORD`** to your MySQL password (overrides `db.password`).

The first time the app starts, it creates tables (if needed) and seeds an admin user plus sample rooms when the `users` table is empty.

## How to run

`run-app.sh` downloads the MySQL JDBC driver into `lib/` on first run (requires internet once).

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
resources/           # Classpath root (database.properties + com/hotel/...)
config/              # Optional db.properties (not committed if secret)
lib/                 # mysql-connector-j (downloaded by run-app.sh)
docs/                # Project documentation
```

## Architecture

- `com.hotel.ui` — Controllers / UI layer  
- `com.hotel.service` — Business logic  
- `com.hotel.database.dao` — Data access  
- `com.hotel.database` — Persistence bootstrap  
- `com.hotel.model` — Domain models  

## Persistent data

All application data is stored in **MySQL** (`users`, `rooms`, `reservations` tables). Ensure the server is running and credentials in `resources/database.properties` or `config/db.properties` match your environment.

## Documentation

See the `docs/` folder:

- `docs/requirements.md`
- `docs/design.md`
- `docs/uml.md`
- `docs/test-document.md`
- `docs/final-report-outline.md`
