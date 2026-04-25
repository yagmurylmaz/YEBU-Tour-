# Test Document

## Test Scope

This document covers functional, UI/UX, persistence, and regression tests for:

- Authentication and account flows
- Customer booking flows
- Admin management/reporting flows
- Dashboard performance/pagination/theme behavior

## Preconditions

- MySQL server is running and reachable with configured credentials
- Database schema is initialized by application startup
- At least one admin user exists (`admin@hotel.com`)
- Sample hotels/rooms exist in database

## Test Scenarios and Cases

| ID | Scenario | Expected Result | Actual Result |
|---|---|---|---|
| T1 | Login with valid admin | Admin dashboard opens | Pending |
| T2 | Login with invalid password | Error message shown | Pending |
| T3 | Register with existing email | Validation error shown | Pending |
| T3.1 | Register with valid new email | Verification flow starts and user can complete registration | Pending |
| T3.2 | Complete registration with wrong code | Error shown, registration not completed | Pending |
| T3.3 | Complete registration with valid code | Account is created and user can login | Pending |
| T3.4 | Forgot password with known email | Reset code is generated/sent and reset flow can continue | Pending |
| T3.5 | Reset password with valid code | Password is updated and new login succeeds | Pending |
| T4 | Search rooms with invalid dates | Date validation warning shown | Pending |
| T4.1 | Search rooms with valid date range | Available rooms list is returned | Pending |
| T4.2 | Search with room type filter | Only matching room types are shown | Pending |
| T5 | Create reservation | Reservation created and listed | Pending |
| T6 | Cancel pending reservation (customer) | Status becomes cancelled | Pending |
| T7 | Approve pending reservation (admin) | Status becomes approved | Pending |
| T7.1 | Approve non-pending reservation | Validation warning shown, no status change | Pending |
| T8 | Add/Update/Delete room | Table reflects CRUD operation | Pending |
| T8.1 | Add room with invalid numeric fields | Validation warning shown, no insert | Pending |
| T8.2 | Update existing room | Room row updates correctly | Pending |
| T8.3 | Delete room used by reservation | Deletion blocked or handled safely | Pending |
| T9 | Restart app after data change | Data persists in MySQL tables | Pending |
| T10 | Toggle customer dashboard dark mode switch | Theme changes immediately and persists in session | Pending |
| T11 | Toggle login-screen dark mode switch | Switch animates and theme matches session state | Pending |
| T12 | Customer dashboard initial listing | Maximum 9 hotel cards visible initially | Pending |
| T13 | Scroll to bottom with remaining hotels | `Load More` button appears only near bottom | Pending |
| T14 | Click `Load More` | Next page of hotels is appended by 9 | Pending |
| T14.1 | Click `Load More` until list end | Button hides when no remaining hotels | Pending |
| T15 | Switch `My Favorites` ↔ `All Hotels` | Transition is fast and cards refresh correctly | Pending |
| T15.1 | Force one render failure, then retry | Skeleton appears again on next retry | Pending |
| T16 | Add hotel to favorites | Favorite icon toggles and filter reflects selection | Pending |
| T16.1 | Remove hotel from favorites while favorites-only mode active | Removed hotel disappears from list | Pending |
| T17 | Dashboard min price label | Price is shown in `₺` format | Pending |
| T18 | Open room search from hotel card (`Book Now`) | Navigates to room-search with selected/preferred hotel context | Pending |
| T19 | Support email action | Mail client opens or fallback info message is shown | Pending |
| T20 | Admin report dashboard summary | Pending/approved/cancelled counts and revenue are consistent with DB | Pending |
| T21 | Unauthorized admin page access as non-admin | Access is denied and user is redirected safely | Pending |
| T22 | Remember-me enabled login | Email persists between sessions; auto-login behavior matches design | Pending |
| T23 | Remember-me disabled login | Email is not persisted | Pending |
| T24 | Hotel review listing in dashboard cards | Review count and average score are displayed correctly | Pending |
| T25 | Image fallback behavior | Missing/invalid image paths fall back without crashing UI | Pending |

## Non-Functional / Regression Checklist

| ID | Check | Expected Result | Status |
|---|---|---|---|
| N1 | Initial dashboard render time | First render completes without freeze | Pending |
| N2 | Favorites filter responsiveness | Switching filters does not re-fetch unchanged card data unnecessarily | Pending |
| N3 | Memory stability after repeated navigation | No obvious memory growth or UI slowdown | Pending |
| N4 | Theme toggle reliability | No visual corruption after multiple toggles/navigations | Pending |
| N5 | Error handling | Failures show user-friendly messages and app remains usable | Pending |

## Defects and Fixes

- D1: `AdminRoomController` catch order issue blocked compile -> fixed by catching `NumberFormatException` before `IllegalArgumentException`.
- D2: No persistence in initial version -> fixed with MySQL persistence in `DatabaseConnection` and DAO operations.
- D3: Favorites/all filter toggle felt slow -> improved by caching precomputed hotel card data in dashboard controller.

## Execution Notes

- Keep `Actual Result` and `Status` updated after each test run.
- Capture screenshots for failed scenarios and reference them in defect records.
- Re-run high-risk flows (auth, reservation, persistence, favorites/theme) after each major merge.
