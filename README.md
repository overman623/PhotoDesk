# PhotoDesk

PhotoDesk is a photo gallery for the pictures already on the device. Albums come from the media store. The main sorting tool is a circle: drag a photo outward and it is filed into the album set for that direction.

## Features

- Album list with cover and photo count
- Photo grid and full-screen paging inside an album
- Create an album folder, then add photos so it appears in the list
- Sort photos by dragging the current photo onto one of eight directions
- Tap a direction label to assign an existing album or a new name
- Skip a photo without moving it
- Remember the eight album assignments

A filed photo is moved into `Pictures/<album>/`. On Android 10 and later, the system may ask once before a photo owned by another app can be moved.

## Build

Open this folder in Android Studio. JDK 17 is required.

| Item | Value |
| --- | --- |
| Package | `com.happyhouse.photo.gallery` |
| Language | Kotlin |
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 35 |

`local.properties` is not in the repository. Android Studio writes the SDK path when it creates this file.

## Screens

- `MainActivity` — album list, new album, and the sort entry
- `AlbumActivity` — photo grid
- `ViewerActivity` — full-screen pager
- `OrganizeActivity` — circular drag sorting
