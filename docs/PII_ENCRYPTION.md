# Verification PII Encryption

The verification API accepts the sensitive fields (national number + biometrics)
inside an encrypted envelope so the raw fingerprint is never exposed in transit,
logs, or storage. This is a **standard JWE** — it is language- and OS-agnostic.
You do **not** need Windows or .NET to integrate; only the SecuGen capture daemon
is Windows-bound (hardware SDK). The encryption itself runs anywhere: Node,
Python, Java, Go, PHP, or .NET on Linux/Ubuntu.

## Scheme

- **Format:** JWE compact serialization
- **Key management:** `alg = RSA-OAEP-256`
- **Content encryption:** `enc = A256GCM`
- **Header:** include the certificate's key id as `kid`
- **Plaintext:** the JSON object

```json
{ "nationalNumber": "...", "biometrics": { "fingerPosition": 1, "image": "<wsqBase64>" } }
```

## 1. Fetch your certificate

Per-tenant — each tenant has its own certificate.

```
GET /api/v1/crypto/certificate         Authorization: Basic base64(clientId:clientSecret)
→ { kid, algorithm, encryption, certificatePem, fingerprintSha256, expiresAt }
```

Also downloadable in the client portal → **API Keys → Encryption Certificate**.
Cache it and re-fetch when the `kid` changes (rotation).

## 2. Encrypt and call verify

Send the JWE as `encryptedPayload`; no plaintext PII in the body:

```
POST /api/v1/verify/identity           Authorization: Basic base64(clientId:clientSecret)
{ "encryptedPayload": "<jwe-compact>" }
```

### Node.js (`jose`)

```js
import { CompactEncrypt, importX509 } from "jose";

async function encryptPii(certPem, kid, nationalNumber, fingerPosition, imageBase64) {
  const key = await importX509(certPem, "RSA-OAEP-256");
  const plaintext = new TextEncoder().encode(
    JSON.stringify({ nationalNumber, biometrics: { fingerPosition, image: imageBase64 } }),
  );
  return await new CompactEncrypt(plaintext)
    .setProtectedHeader({ alg: "RSA-OAEP-256", enc: "A256GCM", kid })
    .encrypt(key);
}
```

### Python (`jwcrypto` + `cryptography`)

```python
import json
from jwcrypto import jwk, jwe
from cryptography import x509
from cryptography.hazmat.primitives import serialization

def encrypt_pii(cert_pem, kid, national_number, finger_position, image_b64):
    cert = x509.load_pem_x509_certificate(cert_pem.encode())
    pub = cert.public_key().public_bytes(
        serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo)
    key = jwk.JWK.from_pem(pub)
    payload = json.dumps({
        "nationalNumber": national_number,
        "biometrics": {"fingerPosition": finger_position, "image": image_b64},
    })
    token = jwe.JWE(payload.encode(),
                    protected=json.dumps({"alg": "RSA-OAEP-256", "enc": "A256GCM", "kid": kid}))
    token.add_recipient(key)
    return token.serialize(compact=True)
```

### .NET on Ubuntu

The bundled [`MotabiqCrypto`](../sanad-secugen-capture/MotabiqCrypto.cs) uses only
cross-platform APIs (`System.Security.Cryptography` + `jose-jwt`). Drop that file
into any `net8.0` (non-`-windows`) project and it builds and runs on Ubuntu —
the `-windows` target on the capture daemon is only for the SecuGen hardware SDK.
Architecture: run the Windows capture daemon on the device that has the reader,
send the captured image to your (Linux) backend, and encrypt there before calling
the API.

## Rollout

During migration the API still accepts the legacy plaintext shape
(`{ nationalNumber, biometrics }`). Once `require_encrypted_pii` is enabled for a
tenant (or globally), only the encrypted form is accepted.
