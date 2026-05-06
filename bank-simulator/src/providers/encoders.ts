import type { CaptureResult } from "./types";
import type { FingerPosition } from "../types";

// Mirrors the four export buttons from FingerCapture (PNG / WSQ / B64 / JSON).
// PNG export downloads the on-screen preview. WSQ export uses the daemon-encoded
// payload (the simulator never WSQ-encodes itself — that's the daemon's job).
//
// In Electron, browser downloads land in the user's Downloads folder.

function timestamp() {
  const d = new Date();
  const pad = (n: number) => n.toString().padStart(2, "0");
  return (
    d.getFullYear().toString() +
    pad(d.getMonth() + 1) +
    pad(d.getDate()) +
    "-" +
    pad(d.getHours()) +
    pad(d.getMinutes()) +
    pad(d.getSeconds())
  );
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function base64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

export function exportPng(result: CaptureResult) {
  const bytes = base64ToBytes(result.pngBase64);
  // mark as ArrayBuffer for the BlobPart signature
  const blob = new Blob([bytes.buffer as ArrayBuffer], { type: "image/png" });
  downloadBlob(blob, `fingerprint-${timestamp()}.png`);
}

export function exportWsq(result: CaptureResult) {
  const bytes = base64ToBytes(result.wsqBase64);
  const blob = new Blob([bytes.buffer as ArrayBuffer], { type: "application/octet-stream" });
  downloadBlob(blob, `fingerprint-${timestamp()}.wsq`);
}

export async function copyPngBase64ToClipboard(result: CaptureResult): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(result.pngBase64);
    return true;
  } catch {
    return false;
  }
}

export async function copyWsqBase64ToClipboard(result: CaptureResult): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(result.wsqBase64);
    return true;
  } catch {
    return false;
  }
}

export type SanadEnvelope = {
  nationalNumber: string;
  biometrics: { fingerPosition: FingerPosition; image: string };
};

export function buildSanadEnvelope(
  result: CaptureResult,
  nationalNumber: string,
): SanadEnvelope {
  return {
    nationalNumber,
    biometrics: {
      fingerPosition: result.fingerPosition,
      image: result.wsqBase64,
    },
  };
}

export function exportJson(result: CaptureResult, nationalNumber: string) {
  const payload = buildSanadEnvelope(result, nationalNumber);
  const json = JSON.stringify(payload, null, 2);
  const blob = new Blob([json], { type: "application/json" });
  downloadBlob(blob, `fingerprint-${timestamp()}.json`);
}
