# Test Document

## Test Scenarios and Cases

| ID | Scenario | Expected Result | Actual Result |
|---|---|---|---|
| T1 | Login with valid admin | Admin dashboard opens | Pending |
| T2 | Login with invalid password | Error message shown | Pending |
| T3 | Register with existing email | Validation error shown | Pending |
| T4 | Search rooms with invalid dates | Date validation warning shown | Pending |
| T5 | Create reservation | Reservation created and listed | Pending |
| T6 | Cancel pending reservation (customer) | Status becomes cancelled | Pending |
| T7 | Approve pending reservation (admin) | Status becomes approved | Pending |
| T8 | Add/Update/Delete room | Table reflects CRUD operation | Pending |
| T9 | Restart app after data change | Data persists in MySQL tables | Pending |
| T10 | Toggle customer dashboard dark mode switch | Theme changes immediately and persists in session | Pending |
| T11 | Toggle login-screen dark mode switch | Switch animates and theme matches session state | Pending |
| T12 | Customer dashboard initial listing | Maximum 9 hotel cards visible initially | Pending |
| T13 | Scroll to bottom with remaining hotels | `Load More` button appears only near bottom | Pending |
| T14 | Click `Load More` | Next page of hotels is appended by 9 | Pending |

## Defects and Fixes

- D1: `AdminRoomController` catch order issue blocked compile -> fixed by catching `NumberFormatException` before `IllegalArgumentException`.
- D2: No persistence in initial version -> fixed with MySQL persistence in `DatabaseConnection` and DAO operations.
- D3: Favorites/all filter toggle felt slow -> improved by caching precomputed hotel card data in dashboard controller.
