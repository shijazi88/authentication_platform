// Vendor-agnostic fingerprint provider contract.
//
// Today: SecuGen (default) + Dermalog (placeholder). Future devices implement
// FingerprintProvider and add a row to PROVIDER_CATALOG. The Verify screen
// and Devices screen consume providers through this interface only — they
// never know about a specific vendor's SDK.

import type { FingerPosition } from "../types";

export type ProviderId = "secugen" | "dermalog" | string;

/** Static metadata about a provider — what we show in the provider picker. */
export type ProviderInfo = {
  id: ProviderId;
  displayName: string;
  vendor: string;
  defaultBaseUrl: string;
  /** True when an actual capture daemon is implemented. False = stub/mock. */
  hardwareReady: boolean;
};

/** Per-instance configuration the user edits in Devices settings. */
export type ProviderConfig = {
  baseUrl: string;
  /** First-frame timeout (ms) — how long we wait for a finger on the sensor. */
  captureTimeoutMs: number;
  /** NFIQ-equivalent threshold the first frame must meet. 0 = no enforcement. */
  qualityThreshold: number;
  /** Total frames to capture; we keep the highest-quality one. */
  multiCaptureCount: number;
};

export type DeviceInfo = {
  connected: boolean;
  model?: string;
  serialNumber?: string;
  imageWidth?: number;
  imageHeight?: number;
  imageDpi?: number;
  /** Provider-specific notes (driver/firmware/SDK version). */
  notes?: string;
};

export type CaptureResult = {
  fingerPosition: FingerPosition;
  /** 0–100. Mirrors SecuGen's GetImageQuality. */
  quality: number;
  width: number;
  height: number;
  dpi: number;
  /** PNG base64 — for on-screen preview only. */
  pngBase64: string;
  /** Raw 8-bit grayscale base64. Useful for reprocessing locally. */
  rawBase64?: string;
  /** WSQ-encoded base64 — what we send to Sanad. */
  wsqBase64: string;
  /** ISO timestamp the daemon stamped at capture time. */
  capturedAt: string;
};

export type CaptureOptions = {
  fingerPosition: FingerPosition;
  qualityThreshold?: number;
  multiCaptureCount?: number;
  timeoutMs?: number;
  /** Browser AbortSignal — wired to the cancel button. */
  signal?: AbortSignal;
};

export interface FingerprintProvider {
  readonly id: ProviderId;
  readonly displayName: string;
  /** Probe the daemon / hardware; returns connection state + device metadata. */
  info(): Promise<DeviceInfo>;
  /** Capture one print. Resolves with the best-of-N frame; rejects on cancel/error. */
  capture(opts: CaptureOptions): Promise<CaptureResult>;
}

export const DEFAULT_CONFIG: ProviderConfig = {
  baseUrl: "http://localhost:9876",
  captureTimeoutMs: 30_000,
  qualityThreshold: 75,
  multiCaptureCount: 5,
};
