
# GoalQuest

## Overview

**GoalQuest** is an Android application designed to help users set, track, and achieve their goals. The app uses MVVM (Model-View-ViewModel) architecture with the Android Navigation Component and a two-tab bottom navigation bar: **Track** and **Wins**.

---

## Project Structure

```
app/
  ├── build.gradle
  ├── google-services.json
  ├── proguard-rules.pro
  └── src/
      ├── main/
      │   ├── AndroidManifest.xml
      │   └── java/com/developerjp/jieungoalsettingapp/
      │       ├── data/
      │       │   ├── DBHelper.java
      │       │   ├── Goal.java
      │       │   └── GoalDetail.java
      │       ├── ui/
      │       │   ├── dashboard/          ← "Track" tab
      │       │   │   ├── DashboardFragment.kt
      │       │   │   ├── DashboardViewModel.kt
      │       │   │   ├── GoalAdapter.kt
      │       │   │   └── AddGoalBottomSheet.kt
      │       │   └── achievements/       ← "Wins" tab
      │       │       ├── AchievementsFragment.kt
      │       │       └── AchievementsViewModel.kt
      │       ├── worker/
      │       │   └── GoalReminderWorker.kt
      │       └── MainActivity.kt
      └── res/
          ├── layout/
          ├── menu/
          ├── navigation/
          ├── values/
          └── ... (drawable, font, mipmap, xml, etc.)
```

---

## Main Components

### 1. Data Layer (`data/`)
- **DBHelper.java**: Manages the SQLite database — CRUD operations for goals and their progress history.
- **Goal.java**: Data model representing a top-level goal (the "specific" dimension).
- **GoalDetail.java**: Data model for a progress entry tied to a goal (measurable, time-bound, timestamp).

### 2. UI Layer (`ui/`)

#### Track tab (`dashboard/`)
The main screen where users view and manage their **active goals**.
- `DashboardFragment.kt`: Displays active goals in a RecyclerView with a filter dropdown. Hosts the FAB and empty-state card.
- `DashboardViewModel.kt`: Fetches and filters active goals (progress < 100%) from the DB. Handles edit, delete, and progress-update dialogs.
- `GoalAdapter.kt`: RecyclerView adapter that renders each goal card with a progress chart and collapsible history.
- `AddGoalBottomSheet.kt`: Bottom sheet dialog for creating a new goal (name + starting progress + target date).

#### Wins tab (`achievements/`)
Shows all **completed goals** (progress reached 100%) and lifetime statistics.
- `AchievementsFragment.kt`: Displays completed goals in a RecyclerView with summary stats (goals started / completed).
- `AchievementsViewModel.kt`: Loads completed goals and exposes total/completed counts. Contains `CompletedGoalsAdapter`.

### 3. Background Work (`worker/`)
- **GoalReminderWorker.kt**: A `WorkManager` worker that fires a weekly notification reminding the user to check their goals.

### 4. Main Activity
- **MainActivity.kt**: Hosts the `BottomNavigationView` and Navigation Component. Initialises Google Mobile Ads SDK, handles notification-permission requests (Android 13+), and schedules the weekly reminder via `WorkManager`.

### 5. Resources (`res/`)
- **layout/**: XML layouts for the activity, fragments, dialogs, bottom sheet, and list items.
- **menu/**: `bottom_nav_menu.xml` — defines the two bottom-nav items (`navigation_dashboard`, `navigation_achievements`).
- **navigation/**: `mobile_navigation.xml` — navigation graph with Dashboard as the start destination.
- **values/**: Colors, strings (`title_dashboard = "Track"`, `title_achievements = "Wins"`), styles, themes, and dimensions.
- **drawable / font / mipmap**: Icons, the app launcher, and custom fonts.

---

## Navigation

The app uses a **2-tab bottom navigation bar**:

| Tab label | Fragment | Purpose |
|-----------|----------|---------|
| Track | `DashboardFragment` | View & manage active goals; create new goals via bottom sheet |
| Wins  | `AchievementsFragment` | View completed goals and lifetime stats |

Navigation is managed by the Android Navigation Component (`mobile_navigation.xml`). `MainActivity` wires `BottomNavigationView` to `NavController` using `setupWithNavController`.

---

## Database

- Local **SQLite** database managed by `DBHelper.java` (singleton).
- **Tables**: `specific_table` (goal names) and `goal_table` (progress entries per goal).
- Completed goals are those whose latest `goal_table` entry has `measurable = 100`.
- Cloud sync via Firebase (see `google-services.json`) is configured but syncing logic is handled separately.

---

## Ads

- **Google Mobile Ads SDK** is initialised in `MainActivity` on a background coroutine.
- Both `DashboardFragment` and `AchievementsFragment` include a banner `AdView`, paused/resumed with the fragment lifecycle.

---

## Notifications & Background Work

- `GoalReminderWorker` runs **once per week** via `WorkManager` (unique work: `"WeeklyGoalReminder"`, policy: KEEP).
- Notification permission is requested at launch on Android 13+ (Tiramisu).

---

## Testing

- Unit tests: `src/test/`
- Instrumented tests: `src/androidTest/`

---

## How to Extend

- **New screen**: Create a Fragment/ViewModel under `ui/`, add it to `mobile_navigation.xml`, and add a menu item to `bottom_nav_menu.xml`.
- **New data fields**: Add columns to `DBHelper.java` and update the relevant data model.
- **UI changes**: Edit or add layouts in `res/layout/`.

---

## Build & Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Build and run on an emulator or physical device.

---

## Contribution Guidelines

- Follow MVVM architecture.
- Use Fragments for new screens.
- Write unit and instrumented tests for new features.
