# Location Reminder

A Todo list app with location reminders that reminds the user to do something when he reaches a specific location. The app will require the user to create an account and login to set and access reminders.

## Getting Started

1. Clone the project to your local machine.
2. Open the project using Android Studio.
3. Fill out google_maps_key in app/src/debug/res/values/google_maps_api.xml. Sample file below
```
<resources>
    <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">FILL_OUT_HERE</string>
</resources>
```


### Installation

Step by step explanation of how to get a dev environment running.

```
1. To enable Firebase Authentication:
        a. Go to the authentication tab at the Firebase console and enable Email/Password and Google Sign-in methods.
        b. download `google-services.json` and add it to the app.
2. To enable Google Maps:
    a. Go to APIs & Services at the Google console.
    b. Select your project and go to APIs & Credentials.
    c. Create a new api key and restrict it for android apps.
    d. Add your package name and SHA-1 signing-certificate fingerprint.
    c. Enable Maps SDK for Android from API restrictions and Save.
    d. Copy the api key to the `google_maps_api.xml`
    Example:
    <resources>
        <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">FILL_OUT_HERE</string>
    </resources>
3. Run the app on your mobile phone or emulator with Google Play Services in it.
```

## Screenshots
<p float="left">
  <img alt="Login" height="auto" src="/screenshots/login_screen.png" width="300"/>
  <img alt="Home screen no data" height="auto" src="/screenshots/main_screen_no_data.png" width="300"/>
  <img alt="Edit screen" height="auto" src="/screenshots/reminder_edit.png" width="300"/>
</p>

<p float="left">
  <img alt="select location" height="auto" src="/screenshots/map_location.png" width="300"/>
  <img alt="Home screen" height="auto" src="/screenshots/main_screen.png" width="300"/>
</p>

## Built With

* [Koin](https://github.com/InsertKoinIO/koin) - A pragmatic lightweight dependency injection framework for Kotlin.
* [FirebaseUI Authentication](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md) - FirebaseUI provides a drop-in auth solution that handles the UI flows for signing
* [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Run background service from the background application

## License
Apache License 2.0
