# Workout Tracker

## About

Workout Tracker is a native Android application for logging strength workouts.  
The app lets users start workouts from predefined plans or from scratch, add exercises and sets, track training volume over time, and review previous sessions with detailed breakdowns.  
It was developed as my bachelor’s thesis project.

---

## Tech Stack

[![Kotlin][Kotlin.com]][Kotlin-url]
[![AndroidStudio][AndroidStudio.com]][AndroidStudio-url]
[![Firebase][Firebase.com]][Firebase-url]

---

## Key Features

- **Statistics**

  - Bar chart with training statistics (duration, volume, number of sets)
  - Multiple time ranges: last 7 days, 30 days, 3 months, 12 months
  - Tap on a bar to see exact values

- **Starting training sessions**

  - Start an empty workout with a custom name
  - Start a workout based on predefined training plans

- **Logging workouts**

  - Add exercises from a predefined list with search by name
  - Exercise recommendations based on current workout, training history and preferences
  - Manage exercises: remove exercises, add/remove sets
  - Log weight, reps and notes per exercise
  - Configure and start a rest timer between sets

- **Workout history**
  - List of completed workouts ordered from newest
  - Summary info per workout: name, start time, duration, volume, number of sets
  - Expand exercise list per workout (“See more”) and open detailed view with full sets data

---

## Technical Highlights

- **Architecture**

  - MVVM (Model-View-ViewModel) pattern
  - Views (mainly fragments) observe `LiveData` exposed by ViewModels and are kept free of business logic
  - Data Binding used to bind UI components directly to ViewModel properties and actions

- **Data layer (Firestore)**

  - `workouts` collection storing completed workouts with exercise and set data embedded
  - `exercises` collection as a reference list of available exercises (primary/secondary muscles, type, equipment)
  - `plans` collection for predefined training plans and their workouts
  - `preferences` collection for storing user behaviour for the recommendation algorithm (last selection, selection count)

- **Exercise recommendation algorithm**

  - Multi-criteria scoring system combining:
    - recency of training specific muscles
    - exercise frequency
    - neglected muscle groups
    - current workout type
    - user preferences (how often recommended exercises are actually chosen)
    - muscle balance within the current workout
  - Each factor is normalised and weighted to produce a final score (0–100) per exercise

- **Navigation and UI**

  - Single `MainActivity` with bottom navigation and three main sections: Home, Workout, History
  - Fragment map with show/hide logic to avoid unnecessary fragment recreation and keep it's data when user switch between sections

- **Performance and testing**
  - Performance measured with Firebase Performance (app start, fragment switching, chart loading, recommendation generation, workouts and exercise loading)
  - Runtime behaviour analysed with Android Profiler (CPU, memory, events)
  - UI test with Espresso for the full “add new workout” flow (start, add exercise, fill data, finish and save)

---

## Getting Started

### 1. Prerequisites

- Android Studio installed

- JDK 8+

- Android emulator or device (Android 10 / API 29 or newer)

- Firebase project with Firestore enabled (check [Firebase for Android](https://firebase.google.com/docs/android/setup?hl=pl) for more info)

- `google-services.json` for your Firebase project placed in the `app/` directory (copy from )

### 2. Clone the repo

```bash
git clone https://github.com/Jacek-Lal/Workout-Tracker.git
```

### 3. Open and run the project

- Open the project in Android Studio (File → Open).

- Add your google-services.json file to the app/ module.

- Sync Gradle and build the project.

- Run the app on a device or emulator (Run → Run 'app').

## Possible additions and improvements

- User authentication and per-user data separation

- More advanced filtering/analytics of workout history

- More extensive testing on a wider range of devices and Android versions

[Kotlin.com]: https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white
[Kotlin-url]: https://kotlinlang.org/
[AndroidStudio.com]: https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android-studio&logoColor=white
[AndroidStudio-url]: https://developer.android.com/studio?hl=pl
[Firebase.com]: https://img.shields.io/badge/firebase-a08021?style=for-the-badge&logo=firebase&logoColor=ffcd34
[Firebase-url]: https://firebase.google.com/
