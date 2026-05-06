import type {
  CaptureOptions,
  CaptureResult,
  DeviceInfo,
  FingerprintProvider,
  ProviderId,
} from "./types";

// Stub provider used for vendors without a working sidecar daemon yet
// (e.g. Dermalog at the time of writing). It deliberately reports the device
// as disconnected so the UI surfaces a clear "device offline" state — no
// fake captures, no synthetic images. When the vendor's daemon is ready,
// flip its `hardwareReady` flag in PROVIDER_CATALOG and the registry will
// route to HttpFingerprintProvider instead.
export class MockFingerprintProvider implements FingerprintProvider {
  constructor(
    public readonly id: ProviderId,
    public readonly displayName: string,
  ) {}

  async info(): Promise<DeviceInfo> {
    return {
      connected: false,
      notes: `${this.displayName} integration is not implemented yet.`,
    };
  }

  async capture(_opts: CaptureOptions): Promise<CaptureResult> {
    throw new Error(
      `${this.displayName} capture is not implemented yet. Plug in a SecuGen device or wait for the ${this.displayName} integration to ship.`,
    );
  }
}
