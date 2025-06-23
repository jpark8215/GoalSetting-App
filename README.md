
# GoalQuest 

## Overview

**GoalQuest** is an Android application designed to help users set, track, and achieve their goals. The app is structured using the MVVM (Model-View-ViewModel) architecture and leverages Fragments for UI navigation.

---

## Project Structure

```
app/
  ├── build.gradle
  ├── google-services.json
  ├── proguard-rules.pro
  ├── src/
  │   ├── main/
  │   │   ├── AndroidManifest.xml
  │   │   ├── java/com/developerjp/jieungoalsettingapp/
  │   │   │   ├── data/
  │   │   │   │   ├── DBHelper.java
  │   │   │   │   ├── Goal.java
  │   │   │   │   └── GoalDetail.java
  │   │   │   ├── MainActivity.kt
  │   │   │   └── ui/
  │   │   │       ├── achievements/
  │   │   │       │   ├── AchievementsFragment.kt
  │   │   │       │   └── AchievementsViewModel.kt
  │   │   │       ├── dashboard/
  │   │   │       │   ├── DashboardFragment.kt
  │   │   │       │   ├── DashboardViewModel.kt
  │   │   │       │   └── GoalAdapter.kt
  │   │   │       └── home/
  │   │   │           └── HomeFragment.kt
  │   │   └── res/
  │   │       └── ... (layouts, drawables, values, etc.)
  │   └── test/
  │       └── ... (unit tests)
  └── ... (other build and config files)
```

---

## Main Components

### 1. Data Layer (`data/`)
- **DBHelper.java**: Handles SQLite database operations (CRUD for goals and details).
- **Goal.java**: Data model representing a goal.
- **GoalDetail.java**: Data model for detailed goal information.

### 2. UI Layer (`ui/`)
- **achievements/**
  - `AchievementsFragment.kt`: UI for displaying user achievements.
  - `AchievementsViewModel.kt`: ViewModel for achievements logic.
- **dashboard/**
  - `DashboardFragment.kt`: UI for listing and managing goals.
  - `DashboardViewModel.kt`: ViewModel for dashboard logic.
  - `GoalAdapter.kt`: RecyclerView adapter for displaying goals.
- **home/**
  - `HomeFragment.kt`: Main landing page fragment.

### 3. Main Activity
- **MainActivity.kt**: Hosts the navigation and fragment container.

### 4. Resources (`res/`)
- **layout/**: XML files for activities, fragments, dialogs, and list items.
- **drawable/**: Icons and images.
- **values/**: Colors, strings, styles, and themes.
- **navigation/**: Navigation graph for fragment transitions.

---

## Navigation

- Uses a bottom navigation bar (`bottom_nav_menu.xml`) to switch between Home, Dashboard, and Achievements.
- Navigation is managed via the Navigation Component (`mobile_navigation.xml`).

---

## Database

- Local SQLite database managed by `DBHelper.java`.
- Stores goals and their details.

---

## Testing

- Instrumented and unit tests are located under `src/androidTest/` and `src/test/`.

---

## How to Extend

- **Add new features**: Create new Fragments/ViewModels under `ui/`.
- **Add new data models**: Place them in the `data/` directory and update `DBHelper.java` as needed.
- **UI changes**: Update or add new layouts in `res/layout/`.

---

## Build & Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Build and run on an emulator or device.

---

## Contribution Guidelines

- Follow MVVM architecture.
- Use Fragments for new screens.
- Write unit and instrumented tests for new features.

---
