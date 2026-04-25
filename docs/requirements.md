# Requirements Document

## Project Purpose

Provide a single GUI application to manage hotel reservation workflows for YEBU Tour.

## Target Users

- Administrator  
- Customer  

## Scope

- Login and role-based navigation  
- Email-based registration verification and password reset  
- Room search and reservation creation  
- View and cancel own reservations  
- Hotel browsing with favorites and reviews  
- Admin: approve / cancel / reports / room management  

## Functional Requirements

- Multi-screen JavaFX UI  
- Login with role-based screens  
- Registration verification and password reset flows  
- Room CRUD  
- Hotel + room browsing with filtering and per-hotel navigation  
- Transaction: create reservation  
- Reporting: counts, revenue, distribution  
- Favorites management and hotel review listing  
- Theme toggle (dark/light mode) on key customer-facing screens  
- Validation, exception handling, user feedback  
- Persistent storage  

## Non-Functional Requirements

- Clear, usable interface  
- Modular layered code  
- Maintainability  
- Consistent naming and responsibilities  

## Assumptions and Constraints

- Single-user desktop scenario  
- MySQL-based persistence  
- External DB is required  
- Optional cloud deployment on AWS (EC2/RDS/S3)
