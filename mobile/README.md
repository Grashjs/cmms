# Atlas CMMS Mobile App

This project aims to help manage assets, schedule maintenance and track work orders. This is the mobile app developed
with React Native.
You can use the [live app](https://play.google.com/store/apps/details?id=com.atlas.cmms) and configure it with your custom server url, or build the app locally

**And please star the repo**.

**Screenshot**:

<img src="https://i.ibb.co/B39dVjC/Screenshot-20230320-110652.jpg" width="300"/>
<img src="https://i.ibb.co/NWSfcpq/Screenshot-20230320-111216.jpg" width="300"/>

## Start/run
```shell
npm run android
npm run ios
```

Before the first iOS build, create the Firebase config file the Xcode project
expects as a build input:

```shell
cp ios/AtlasCMMS/GoogleService-Info.plist.example ios/AtlasCMMS/GoogleService-Info.plist
```

The real file is gitignored, so a fresh clone fails with `Build input file
cannot be found` until you do this. The placeholder values are enough to build
and run against a self-hosted backend — `AppDelegate.swift` only calls
`FirebaseApp.configure()` once `API_KEY` holds a genuine key, so analytics and
push stay off until you supply one.

## Configuration

Set these environment variables in the command line or creating a `.env` file

| Name       | Required | Description         | Default Value |
|------------|----------|---------------------|---------------|
| API_URL    | Yes      | Your public api url | (empty)       |

## Build
### Setup
- Create a firebase project. Export `google-services.json`.
- Place the json file in `android/app`
- For iOS, export `GoogleService-Info.plist` from the same project and place it
  in `ios/AtlasCMMS`, replacing the placeholder copied above.
- Create an [expo](https://expo.dev) account.
- Run `npm install -g eas-cli`
### Generate apk
```shell
eas build --profile previewAndroid --platform android
```
It will generate an apk in expo.
## Getting help

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker or send an
email at ibracool99@gmail.com.

## Getting involved

You can contribute in different ways. Sending feedback on features, fixing certain bugs, implementing new features, etc.
Instructions on _how_ to contribute can be found in [CONTRIBUTING](CONTRIBUTING.md).
