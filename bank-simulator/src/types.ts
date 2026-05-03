export type FingerPosition = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10;

export const FINGER_LABELS: Record<FingerPosition, string> = {
  1: "Right thumb",
  2: "Right index",
  3: "Right middle",
  4: "Right ring",
  5: "Right little",
  6: "Left thumb",
  7: "Left index",
  8: "Left middle",
  9: "Left ring",
  10: "Left little",
};

export type VerifyRequest = {
  nationalNumber: string;
  biometrics?: {
    fingerPosition: FingerPosition;
    image: string; // base64, no data-url prefix
  };
};

/**
 * MOI verification verdict. Real MOI returns these strings under
 * {@code verification.verification}; older mocks used {@code verification.status}.
 */
export type VerificationVerdict =
  | "MATCH"
  | "NO_MATCH"
  | "NO_VERIFICATION_POSSIBLE"
  | string;

/** Yemeni-MOI name structure — 4 parts plus a derived {@code full}. */
export type NameParts = {
  first?: string | null;
  second?: string | null;
  third?: string | null;
  fourth?: string | null;
  last?: string | null;
  full?: string | null;
};

/**
 * Address block returned by MOI under
 * {@code person.demographics.addresses.{birth|home|work}}.
 * Birth and home share the same shape; work is reduced.
 */
export type Address = {
  village?: string | null;
  locality?: string | null;
  city?: string | null;
  state?: string | null;
  country?: { id?: string; text?: string | null };
  address?: string | null;
  extra?: string | null;
  placeOfWork?: string | null;
};

/**
 * Verification block. Both shapes accepted: real MOI uses
 * {@code verification.verification} (string) + {@code biometrics} (plural,
 * with {@code exists}). The older wiremock fixture used
 * {@code verification.status} + {@code biometric} (singular, with
 * {@code status} boolean).
 */
export type VerificationBlock = {
  verification?: VerificationVerdict;
  status?: VerificationVerdict;
  biometrics?: { exists?: boolean; score?: number | null };
  biometric?: { status?: boolean; score?: number | null };
};

export type VerifyResponse = {
  transaction: {
    id: string;
    timestamp: string;
    status?: "OK" | "FAILED" | string;
  };
  result?: {
    transaction?: { id?: string; timestamp?: string };
    verification?: VerificationBlock;
    person?: {
      nationalNumber?: string;
      cards?: Array<{
        documentNumber?: string;
        type?: string;
        serialNumber?: string;
        status?: string;
        expiry?: string;
        issue?: string;
        issuingAuthority?: string;
      }>;
      demographics?: {
        names?: { arabic?: NameParts; latin?: NameParts };
        phone?: string;
        email?: string;
        dateOfBirth?: string;
        gender?: { id?: string; text?: string };
        maritalStatus?: string;
        profession?: string;
        religion?: string;
        educationLevel?: string;
        nationality?: { id?: string; text?: string | null };
        addresses?: {
          birth?: Address;
          home?: Address;
          work?: Address;
        };
      };
    };
    [k: string]: unknown;
  };
};

export type ApiError = {
  error?: string;
  message?: string;
  code?: string;
};

export type HistoryEntry = {
  id: string;
  ts: string;
  nationalNumber: string;
  fingerPosition?: FingerPosition;
  success: boolean;
  httpStatus?: number;
  verificationStatus?: string;
  error?: string;
  // Full request/response snapshots for audit
  endpoint: string; // e.g. "POST /api/v1/verify/identity"
  baseUrl: string;
  requestBody: unknown; // the JSON sent (image truncated for storage)
  responseBody?: unknown; // the JSON received (full response)
  durationMs?: number;
};
