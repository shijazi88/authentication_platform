# sanad-secugen-capture

A small headless HTTP daemon that bridges a USB-attached **SecuGen Hamster Pro 20**
(or any HU20-family device using the FDx SDK Pro v4.3.1) to the
[Sanad Bank Simulator](../bank-simulator/) over HTTP.

The simulator's renderer cannot talk to the SecuGen .NET DLLs directly, so this
process owns the device handle and exposes capture/info as JSON endpoints on
`localhost:9876`. The simulator's `HttpFingerprintProvider` is the only client.

## When the daemon runs

- **Local dev (your laptop):** The Electron main process auto-spawns this daemon
  on app start (see [`bank-simulator/electron/main.ts`](../bank-simulator/electron/main.ts)).
  No manual launch required.
- **Production install:** `electron-builder` copies the published daemon into the
  Sanad Bank Simulator installer (`extraResources` in
  [`bank-simulator/package.json`](../bank-simulator/package.json#L40)). The
  installed simulator launches it the same way.

If you don't have a SecuGen plugged in, the simulator still works — file upload,
sample fingerprint, and history are unaffected. Only "Capture from device"
returns disconnected.

## Platform requirements

| | |
|---|---|
| OS | Windows 10/11 x64 |
| .NET | 8.0 SDK (build) · runtime is bundled (`--self-contained true`) for distribution |
| Hardware | SecuGen Hamster Pro 20 / HU20-family scanner over USB |
| Driver | FDx SDK Pro v4.3.1 native DLLs (already bundled in [`sdk-lib/`](sdk-lib/)) |

The daemon is Windows-only because the SecuGen SDK is Windows-only. On macOS/Linux,
the Electron main process detects the platform and skips the daemon spawn — the
simulator still runs without device capture.

## Build

```powershell
dotnet build
```

Produces `bin/Debug/net8.0-windows/sanad-secugen-capture.exe`.

## Run (manual, for daemon-side debugging)

```powershell
dotnet run
```

Or run the .exe directly. Listens on `http://localhost:9876` by default.

## Publish (for the installer)

Done automatically by the bank-simulator build script
(`npm run build` → `build:daemon`). To do it manually:

```powershell
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=false -o publish
```

Output goes to `publish/` (gitignored). `electron-builder` copies it under
`resources/sanad-secugen-capture/` inside the NSIS installer.

## HTTP API

| Method | Path | Body | Returns |
|---|---|---|---|
| `GET` | `/info` | — | `DeviceInfoDto` — connected/model/serial/dimensions/dpi |
| `POST` | `/capture` | `CaptureRequestDto` | `CaptureResponseDto` — quality + PNG/WSQ/raw base64 |
| `POST` | `/capture/cancel` | — | `204` (best-effort cancellation of in-flight capture) |
| `GET` | `/` | — | Daemon name/version |

DTO shapes are in [`Dtos.cs`](Dtos.cs). The TypeScript client side mirrors them
in [`bank-simulator/src/providers/types.ts`](../bank-simulator/src/providers/types.ts).

## Behavior worth knowing

- **Single-reader USB:** Captures are gated by a `SemaphoreSlim`. A `/capture`
  in flight blocks any concurrent `/info` or `/capture` until it finishes.
- **Disconnect detection:** Every `/info` call closes the SDK handle and
  re-enumerates the USB bus. SecuGen's enumeration is unreliable while a handle
  is open, so this is the only way to detect unplug events accurately.
- **Image enhancement:** The PNG returned to the simulator is auto-contrast +
  gamma 0.85 corrected for visibility. The WSQ payload is the raw sensor bytes —
  the matcher needs unprocessed data.

## Adding a new vendor

The simulator's provider abstraction is vendor-agnostic. To add (e.g.) Dermalog:

1. Build a new daemon (any language) that exposes the same three endpoints with
   the same DTOs.
2. Add a row to
   [`bank-simulator/src/providers/registry.ts`](../bank-simulator/src/providers/registry.ts)
   pointing at its port.
3. Flip its `hardwareReady: true`. The simulator's `HttpFingerprintProvider`
   talks to it without further changes.
