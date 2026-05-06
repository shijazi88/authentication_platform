using System.Text.Json;
using System.Text.Json.Serialization;
using SanadSecuGenCapture;

// HTTP daemon that bridges the SecuGen FDx SDK to the Sanad Bank Simulator.
// Listens on localhost:9876 by default. Bank simulator's HttpFingerprintProvider
// (bank-simulator/src/providers/HttpFingerprintProvider.ts) speaks the same contract:
//   GET  /info            → DeviceInfoDto
//   POST /capture         ← CaptureRequestDto, → CaptureResponseDto
//   POST /capture/cancel  → 204 (best-effort cancellation)
//
// The daemon is bundled inside the bank simulator's Windows installer and
// auto-spawned by Electron's main process on app launch.

var builder = WebApplication.CreateBuilder(args);

// Loopback only by default. Override via Kestrel:Endpoints in appsettings.json.
builder.WebHost.ConfigureKestrel(o =>
{
    int port = builder.Configuration.GetValue<int?>("Port") ?? 9876;
    o.ListenLocalhost(port);
});

// CORS — the simulator runs in Electron at file:// or http://localhost:5173 (dev).
// Permissive for any localhost origin; the daemon only listens on loopback anyway.
builder.Services.AddCors(o => o.AddDefaultPolicy(p =>
    p.SetIsOriginAllowed(origin =>
    {
        if (string.IsNullOrEmpty(origin)) return true;
        if (origin == "null") return true; // file:// produces "null"
        if (Uri.TryCreate(origin, UriKind.Absolute, out var u))
        {
            return u.Host is "localhost" or "127.0.0.1" or "::1";
        }
        return false;
    })
    .AllowAnyMethod()
    .AllowAnyHeader()));

builder.Services.AddSingleton<SecuGenCapture>();

// PascalCase → camelCase to match the simulator's TypeScript DTOs without a
// hand-written serializer on either side. Passed explicitly to each Results.Json call.
var jsonOptions = new JsonSerializerOptions
{
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
};

var app = builder.Build();
app.UseCors();

// Track the in-flight capture so /capture/cancel can abort it.
CancellationTokenSource? inflightCts = null;
var inflightLock = new object();

app.MapGet("/info", async (SecuGenCapture cap, CancellationToken ct) =>
{
    var info = await cap.GetInfoAsync(ct);
    return Results.Json(info, jsonOptions);
});

app.MapPost("/capture", async (CaptureRequestDto req, SecuGenCapture cap, CancellationToken httpCt) =>
{
    if (req.FingerPosition < 1 || req.FingerPosition > 10)
    {
        return Results.BadRequest(new ErrorDto("Invalid fingerPosition", "Must be 1–10"));
    }

    using var captureCts = CancellationTokenSource.CreateLinkedTokenSource(httpCt);
    lock (inflightLock) { inflightCts = captureCts; }

    try
    {
        var result = await cap.CaptureAsync(req, captureCts.Token);
        return Results.Json(result, jsonOptions);
    }
    catch (OperationCanceledException)
    {
        // 499 isn't in StatusCodes constants (it's an Nginx-ism for client-closed-request).
        return Results.StatusCode(499);
    }
    catch (Exception ex)
    {
        return Results.Json(
            new ErrorDto(ex.Message),
            jsonOptions,
            statusCode: StatusCodes.Status500InternalServerError);
    }
    finally
    {
        lock (inflightLock) { if (inflightCts == captureCts) inflightCts = null; }
    }
});

app.MapPost("/capture/cancel", () =>
{
    lock (inflightLock) { inflightCts?.Cancel(); }
    return Results.NoContent();
});

app.MapGet("/", () => Results.Ok(new
{
    name = "sanad-secugen-capture",
    version = "0.1.0",
    endpoints = new[] { "GET /info", "POST /capture", "POST /capture/cancel" }
}));

app.Run();
