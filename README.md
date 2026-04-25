# YEBU Tour - Reservation System

JavaFX desktop application for hotel reservation management.

## Recent UI updates

- **Customer Dashboard prices** now display in Turkish Lira (`₺`) instead of dollar.
- **Favorites filter performance** improved with card-data caching, so switching between `My Favorites` and `All Hotels` is much faster.
- **Hotel list pagination** is now button-based:
  - Initially shows up to 9 hotels
  - Remaining hotels load with `Load More`
  - `Load More` appears only near the bottom of the list
- **Dark mode switch** added as a sliding toggle:
  - Available on login and customer dashboard screens
  - Theme state is shared through session, so screens stay in sync

## How to run

`run-app.sh` downloads the MySQL JDBC driver into `lib/` on first run (requires internet once).

```bash
# clone/download the project, then open terminal in project root
chmod +x run-app.sh
./run-app.sh
```

Database connection defaults are in `resources/database.properties`. You can optionally override them with `config/db.properties` or `YEBU_DB_PASSWORD`.

On first startup, the app initializes required tables and seeds default data when necessary.

## Cloud deployment (AWS)

This project can also be deployed on **AWS** for demo/production environments.

- **Application host:** AWS EC2
- **Database:** AWS RDS (MySQL)
- **Media/static assets (optional):** AWS S3

> Keep credentials and connection strings in environment variables or non-committed config files.

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

All application data is stored in **MySQL**. Core tables include `users`, `hotels`, `rooms`, `reservations`, `hotel_reviews`, `favorite_hotels`, and related lookup/media tables. Ensure the server is running and credentials in `resources/database.properties` or `config/db.properties` match your environment.

## Documentation

See the `docs/` folder:

- `docs/requirements.md`
- `docs/design.md`
- `docs/uml.md`
- `docs/test-document.md`
- `docs/final-report-outline.md`
