# Cocktail App
Android app to keep all your cocktail recipes on your phone. Sign up, browse a Firestore-backed catalogue, read histories and ingredients, rate drinks, save favorites, search/filter by ingredients, and pull random suggestions. Tutorials are included for techniques.

## Features
- Email/password auth with Firebase; login/registration on first screen.
- Browse all cocktails and open detail pages with history, expert rating, and ingredient list.
- Search by name and filter by one or more ingredients; results update live.
- Get a random cocktail that matches selected ingredients.
- Save/remove favorites and view them in a dedicated list.
- Rate cocktails; ratings are stored per user and cocktail.
- Read tutorials pulled from Firestore and open detailed views.
- Offline cache enabled for Firestore reads (uses local data when offline).

## Tech Stack
- Kotlin, Android SDK 35 (minSdk 24), Gradle.
- XML layouts with ViewBinding and RecyclerView adapters.
- Firebase Auth, Cloud Firestore, Analytics (via `google-services.json`).
- Material Components; UI is XML-based.

## Project Layout
- `app/src/main/java/com/example/cocktailapp/ui/activities/` - screens (login, main menu, list, detail, favorites, search, random, tutorials).
- `app/src/main/java/com/example/cocktailapp/adapters/` - RecyclerView adapters for cocktails and tutorials.
- `app/src/main/java/com/example/cocktailapp/models/` - data models (`Cocktail`, `Ingredient`, `User`, `Tutorial`).
- `app/src/main/java/com/example/cocktailapp/CocktailApp.kt` - enables Firestore offline cache.
- `app/src/main/res/layout/` - XML layouts for activities and list items.

## Firebase Setup
1) Create a Firebase project and enable Email/Password authentication.  
2) Add an Android app with package id `com.example.cocktailapp`, download `google-services.json`, and place it at `app/google-services.json` (replace the placeholder if committed).  
3) Enable Cloud Firestore in Production mode.

Firestore collections expected by the app (field names are lowercase):
- `cocktails` - doc id is the cocktail identifier used everywhere (for example `old_fashioned`). Fields: `name` (string), `flavourDescription` (string), `history` (string), `expertRating` (number), `ingredients` (array of objects `{ name: string, quantity: string }`).
- `users` - doc id = Firebase `uid`. Fields: `favorites` (array of cocktail doc ids). Other profile fields optional.
- `ratings` - doc id pattern `<uid>-<cocktailId>`. Fields: `userId`, `cocktailId`, `cocktailName`, `rating` (number).
- `Tutorials` - doc id = tutorial name/title. Field: `Tuto` (string description).

If your existing data uses `Ingredients`/`Name`/`Quantity` or stores favorites by cocktail name, update it to the schema above so the app can read it safely.

### Firestore security rules
- Recommended rules are in `firestore.rules`. Deploy with Firebase CLI: `firebase deploy --only firestore:rules`.
- Behavior: cocktails/tutorials are public read-only; each user can read/update only their document; ratings require authentication and must belong to the authenticated user.

## Run the App
- CLI build: `gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` (macOS/Linux).
- Android Studio: open the project, let it sync, then Run or Build > Build APK(s).

## Build an APK to test on your phone
- Android Studio: Build > Build Bundle(s) / APK(s) > Build APK(s). Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- CLI (Windows): `gradlew.bat assembleDebug` (macOS/Linux: `./gradlew assembleDebug`). Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Transfer the APK to your phone (USB, Drive, AirDrop, etc.) and open it to install. Enable "Install unknown apps" on the device if prompted.
- If you prefer an install via USB with adb enabled on the phone: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Testing
- Unit tests: `./gradlew test`
- Instrumented tests (if added later): `./gradlew connectedAndroidTest`
