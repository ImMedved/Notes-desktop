# Notes Desktop

Standalone Windows desktop client project for notes and timers.

## Contents

- Java 17 desktop widget
- Local config and cache storage
- HTTP client for `Notes Server`
- `jpackage` build script for Windows app-image

## Build

```powershell
mvn -q -DskipTests package
```

## Build EXE

```powershell
powershell -ExecutionPolicy Bypass -File .\build-exe.ps1
```

Output:

- `dist\NotesWidgetClient\NotesWidgetClient.exe`

## First local run

By default the client points to:

- server URL: `http://127.0.0.1:8080`
- API key: `change-me`

## Docs

- [API contract](../docs/contracts/api.md)
- [Models](../docs/contracts/models.md)
