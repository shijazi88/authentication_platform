// HTTP request/response shapes shared with the bank-simulator's
// HttpFingerprintProvider (bank-simulator/src/providers/HttpFingerprintProvider.ts).
// Contract: GET /info → DeviceInfoDto, POST /capture → CaptureResponseDto.

namespace SanadSecuGenCapture;

public sealed record DeviceInfoDto(
    bool Connected,
    string? Model,
    string? SerialNumber,
    int? ImageWidth,
    int? ImageHeight,
    int? ImageDpi,
    string? Notes
);

public sealed record CaptureRequestDto(
    int FingerPosition,
    int? QualityThreshold,
    int? MultiCaptureCount,
    int? TimeoutMs
);

public sealed record CaptureResponseDto(
    int FingerPosition,
    int Quality,
    int Width,
    int Height,
    int Dpi,
    string PngBase64,
    string? RawBase64,
    string WsqBase64,
    string CapturedAt
);

public sealed record ErrorDto(string Error, string? Detail = null);
