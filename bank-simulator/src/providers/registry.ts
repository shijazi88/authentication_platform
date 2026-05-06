import { HttpFingerprintProvider } from "./HttpFingerprintProvider";
import { MockFingerprintProvider } from "./MockFingerprintProvider";
import {
  DEFAULT_CONFIG,
  type FingerprintProvider,
  type ProviderConfig,
  type ProviderId,
  type ProviderInfo,
} from "./types";

// Catalogue of supported vendors. Adding a new device:
//   1. Append a row here.
//   2. Ship a sidecar daemon for it (see HttpFingerprintProvider for the contract).
//   3. The provider picker in DevicesScreen will surface it automatically.
//
// hardwareReady=true  → use the HTTP daemon (real device).
// hardwareReady=false → fall back to MockFingerprintProvider (simulation).
//
// Until the sanad-*-capture.exe sidecar daemons ship, every provider stays
// hardwareReady=false so the simulator runs end-to-end without hardware.
// Flip the flag to true once that vendor's daemon is bundled.
export const PROVIDER_CATALOG: ProviderInfo[] = [
  {
    id: "secugen",
    displayName: "SecuGen Hamster Pro 20",
    vendor: "SecuGen",
    defaultBaseUrl: "http://localhost:9876",
    hardwareReady: true,
  },
  {
    id: "dermalog",
    displayName: "Dermalog ZF1",
    vendor: "Dermalog",
    defaultBaseUrl: "http://localhost:9877",
    hardwareReady: false,
  },
];

export const DEFAULT_PROVIDER_ID: ProviderId = "secugen";

export function getProviderInfo(id: ProviderId): ProviderInfo | undefined {
  return PROVIDER_CATALOG.find((p) => p.id === id);
}

export function defaultConfigFor(id: ProviderId): ProviderConfig {
  const info = getProviderInfo(id);
  return { ...DEFAULT_CONFIG, baseUrl: info?.defaultBaseUrl ?? DEFAULT_CONFIG.baseUrl };
}

// Build the right concrete provider for a given (id, config).
// hardwareReady=false providers fall back to Mock so the UI is testable end-to-end.
export function buildProvider(
  id: ProviderId,
  config: ProviderConfig,
): FingerprintProvider {
  const info = getProviderInfo(id);
  if (!info) {
    return new MockFingerprintProvider(id, id);
  }
  if (!info.hardwareReady) {
    return new MockFingerprintProvider(info.id, info.displayName);
  }
  return new HttpFingerprintProvider(info.id, info.displayName, config);
}
