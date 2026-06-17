// MOTABIQ — client-side PII encryption for the verification API.
//
// Encrypts the sensitive verification payload (national number + biometrics)
// into a JWE (RSA-OAEP-256 + A256GCM) using the tenant's encryption
// certificate, so the raw fingerprint never leaves this process in clear.
// The MOTABIQ gateway decrypts it server-side with the tenant private key.
//
// Usage:
//   var cert = await MotabiqCrypto.FetchCertificateAsync(http, baseUrl, clientId, clientSecret);
//   string jwe = MotabiqCrypto.EncryptPii(cert.CertificatePem, cert.Kid,
//                    nationalNumber, fingerPosition, wsqBase64);
//   // POST { "encryptedPayload": jwe } to /api/v1/verify/identity

using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Text.Json;
using Jose;

namespace SanadSecuGenCapture;

public sealed record MotabiqCertificate(
    string Kid,
    string Algorithm,
    string Encryption,
    string CertificatePem,
    string FingerprintSha256,
    string? ExpiresAt
);

public static class MotabiqCrypto
{
    /// <summary>Fetches the tenant's active encryption certificate from the gateway.</summary>
    public static async Task<MotabiqCertificate> FetchCertificateAsync(
        HttpClient http, string baseUrl, string clientId, string clientSecret,
        CancellationToken ct = default)
    {
        using var req = new HttpRequestMessage(HttpMethod.Get,
            baseUrl.TrimEnd('/') + "/api/v1/crypto/certificate");
        var basic = Convert.ToBase64String(Encoding.UTF8.GetBytes($"{clientId}:{clientSecret}"));
        req.Headers.Authorization = new AuthenticationHeaderValue("Basic", basic);

        using var resp = await http.SendAsync(req, ct);
        resp.EnsureSuccessStatusCode();
        var cert = await resp.Content.ReadFromJsonAsync<MotabiqCertificate>(cancellationToken: ct)
                   ?? throw new InvalidOperationException("Empty certificate response");
        return cert;
    }

    /// <summary>
    /// Builds the encrypted payload (JWE compact) for a verify request. The
    /// plaintext is {nationalNumber, biometrics:{fingerPosition, image}}.
    /// </summary>
    public static string EncryptPii(
        string certificatePem, string kid,
        string nationalNumber, int fingerPosition, string imageBase64)
    {
        var payload = new
        {
            nationalNumber,
            biometrics = new { fingerPosition, image = imageBase64 },
        };
        string json = JsonSerializer.Serialize(payload);

        using var cert = X509Certificate2.CreateFromPem(certificatePem);
        using RSA rsa = cert.GetRSAPublicKey()
                        ?? throw new InvalidOperationException("Certificate has no RSA public key");

        var headers = new Dictionary<string, object> { ["kid"] = kid };
        return JWT.Encode(json, rsa, JweAlgorithm.RSA_OAEP_256, JweEncryption.A256GCM,
            extraHeaders: headers);
    }
}
