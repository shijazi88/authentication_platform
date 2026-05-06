using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.Versioning;
using System.Text;
using SecuGen.FDxSDKPro.Windows;

namespace SanadSecuGenCapture;

// Thread-safe wrapper around the SecuGen FDx SDK Pro v4.3.1.
//
// One device handle is opened on first use and shared across requests; only
// one capture can run at a time (USB pipe is single-reader). The capture
// algorithm mirrors what worked in FingerCapture — a multi-frame loop where
// the first frame enforces a quality threshold and the rest are quick re-grabs.
//
// On any non-recoverable SDK error we close the handle so the next request
// gets a fresh init — the device sometimes drops state when it's unplugged
// and plugged back in.
[SupportedOSPlatform("windows")]
public sealed class SecuGenCapture : IDisposable
{
    private readonly SemaphoreSlim _gate = new(1, 1);
    private SGFingerPrintManager? _fpm;
    private SGFPMDeviceName _deviceName;
    private int _deviceId;
    private int _imgWidth;
    private int _imgHeight;
    private int _imgDpi;
    private string _serialNumber = "";
    private string _modelName = "";
    private bool _initialised;

    public async Task<DeviceInfoDto> GetInfoAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct);
        try
        {
            // Force a fresh USB scan on every /info call. SecuGen's
            // EnumerateDevice can return cached counts while a handle is
            // open, so we close any existing handle first — the re-init
            // path will then re-enumerate (live) and re-open (or fail
            // cleanly with "no device detected" when unplugged).
            // Cost: a few ms per call; accuracy: always correct.
            CloseDevice();

            if (!TryEnsureInitialised(out var error))
            {
                return new DeviceInfoDto(false, null, null, null, null, null, error);
            }
            return new DeviceInfoDto(
                Connected: true,
                Model: _modelName,
                SerialNumber: _serialNumber,
                ImageWidth: _imgWidth,
                ImageHeight: _imgHeight,
                ImageDpi: _imgDpi,
                Notes: "SecuGen FDx SDK Pro v4.3.1"
            );
        }
        finally
        {
            _gate.Release();
        }
    }

    public async Task<CaptureResponseDto> CaptureAsync(
        CaptureRequestDto req,
        CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct);
        try
        {
            if (!TryEnsureInitialised(out var error))
            {
                throw new InvalidOperationException(error ?? "Device not ready");
            }

            // Same defaults FingerCapture used — proven to produce NFIQ ≤ 2 prints.
            int qualityThreshold = req.QualityThreshold ?? 75;
            int multiCaptureCount = Math.Clamp(req.MultiCaptureCount ?? 5, 1, 10);
            int firstFrameTimeoutMs = req.TimeoutMs ?? 30_000;

            byte[] bestImage = new byte[_imgWidth * _imgHeight];
            int bestQ = -1;

            for (int i = 0; i < multiCaptureCount; i++)
            {
                ct.ThrowIfCancellationRequested();

                var frame = new byte[_imgWidth * _imgHeight];
                int timeout = i == 0 ? firstFrameTimeoutMs : 2_500;
                int qThresh = i == 0 ? qualityThreshold : 0;

                int rc = _fpm!.GetImageEx(frame, timeout, 0, qThresh);
                if (rc != (int)SGFPMError.ERROR_NONE)
                {
                    if (i == 0)
                    {
                        // First-frame failure is fatal — likely no finger, sensor
                        // dirty, or quality too low. Surface to caller.
                        throw new InvalidOperationException(
                            $"GetImageEx failed: {(SGFPMError)rc} (timeout {timeout}ms, threshold {qThresh})");
                    }
                    continue;
                }

                int q = 0;
                _fpm.GetImageQuality(_imgWidth, _imgHeight, frame, ref q);
                if (q > bestQ)
                {
                    bestQ = q;
                    bestImage = frame;
                }
            }

            if (bestQ < 0)
            {
                throw new InvalidOperationException("No frame met the quality threshold");
            }

            // PNG is for preview only — apply contrast/gamma so the ridges are
            // visible to the operator. WSQ + raw stay pristine for the matcher.
            byte[] enhancedForPreview = EnhanceForPreview(bestImage);
            string pngB64 = EncodeRawAsPng(enhancedForPreview, _imgWidth, _imgHeight);
            string wsqB64 = EncodeRawAsWsq(bestImage, _imgWidth, _imgHeight, _imgDpi);
            string rawB64 = Convert.ToBase64String(bestImage);

            return new CaptureResponseDto(
                FingerPosition: req.FingerPosition,
                Quality: bestQ,
                Width: _imgWidth,
                Height: _imgHeight,
                Dpi: _imgDpi,
                PngBase64: pngB64,
                RawBase64: rawB64,
                WsqBase64: wsqB64,
                CapturedAt: DateTimeOffset.UtcNow.ToString("o")
            );
        }
        catch
        {
            // Drop the handle so the next call re-initialises.
            CloseDevice();
            throw;
        }
        finally
        {
            _gate.Release();
        }
    }

    private bool TryEnsureInitialised(out string? error)
    {
        if (_initialised && _fpm != null)
        {
            error = null;
            return true;
        }

        try
        {
            _fpm = new SGFingerPrintManager();
            int rc = _fpm.EnumerateDevice();
            if (rc != (int)SGFPMError.ERROR_NONE)
            {
                error = $"EnumerateDevice failed: {(SGFPMError)rc}";
                _fpm = null;
                return false;
            }
            if (_fpm.NumberOfDevice == 0)
            {
                error = "No SecuGen device detected. Plug in the scanner and try again.";
                _fpm = null;
                return false;
            }

            var info = new SGFPMDeviceList();
            _fpm.GetEnumDeviceInfo(0, info);
            _deviceName = info.DevName;
            _deviceId = info.DevID;

            rc = _fpm.Init(_deviceName);
            if (rc != (int)SGFPMError.ERROR_NONE)
            {
                error = $"Init failed: {(SGFPMError)rc}";
                _fpm = null;
                return false;
            }

            rc = _fpm.OpenDevice(_deviceId);
            if (rc != (int)SGFPMError.ERROR_NONE)
            {
                error = $"OpenDevice failed: {(SGFPMError)rc}";
                _fpm = null;
                return false;
            }

            var devInfo = new SGFPMDeviceInfoParam();
            _fpm.GetDeviceInfo(devInfo);
            _imgWidth = devInfo.ImageWidth;
            _imgHeight = devInfo.ImageHeight;
            _imgDpi = devInfo.ImageDPI;
            // DeviceSN is a fixed-length byte[] (typically 16). Decode + trim
            // null padding so it's a clean string for the JSON response.
            _serialNumber = devInfo.DeviceSN is { Length: > 0 } sn
                ? Encoding.ASCII.GetString(sn).TrimEnd('\0').Trim()
                : "";
            _modelName = _deviceName.ToString();

            // CRITICAL: disable in-SDK enhancement so the matcher receives raw pixels.
            _fpm.EnableSmartCapture(false);
            _fpm.EnableCheckOfFingerLiveness(false);
            _fpm.SetFakeDetectionLevel(1);

            _initialised = true;
            error = null;
            return true;
        }
        catch (Exception ex)
        {
            error = $"Init exception: {ex.Message}";
            _fpm = null;
            _initialised = false;
            return false;
        }
    }

    // Cosmetic enhancement for the preview image.
    //   1. Histogram stretch using the 2nd / 98th percentiles — auto-contrast
    //      that ignores rare outlier pixels that would otherwise skew the range.
    //   2. Mild gamma (<1) that brightens midtones so ridges stand out.
    // The output is still 8-bit grayscale at the same dimensions, so it can be
    // PNG-encoded with the existing path. WSQ and raw bytes are NOT touched.
    private static byte[] EnhanceForPreview(byte[] src)
    {
        // Build histogram
        var hist = new int[256];
        for (int i = 0; i < src.Length; i++) hist[src[i]]++;

        int total = src.Length;
        int lowCutoff = (int)(total * 0.02);
        int highCutoff = (int)(total * 0.98);

        int low = 0, high = 255;
        int running = 0;
        for (int v = 0; v < 256; v++)
        {
            running += hist[v];
            if (running >= lowCutoff) { low = v; break; }
        }
        running = 0;
        for (int v = 0; v < 256; v++)
        {
            running += hist[v];
            if (running >= highCutoff) { high = v; break; }
        }
        if (high <= low) { low = 0; high = 255; }

        // Precompute the lookup table for the stretch + gamma curve so we
        // don't pay Math.Pow per pixel.
        const double gamma = 0.85;
        var lut = new byte[256];
        double range = high - low;
        for (int v = 0; v < 256; v++)
        {
            double n = (v - low) / range;        // 0..1 (clamped below)
            if (n < 0) n = 0;
            else if (n > 1) n = 1;
            double g = Math.Pow(n, gamma);
            int outVal = (int)Math.Round(g * 255.0);
            if (outVal < 0) outVal = 0;
            else if (outVal > 255) outVal = 255;
            lut[v] = (byte)outVal;
        }

        var dst = new byte[src.Length];
        for (int i = 0; i < src.Length; i++) dst[i] = lut[src[i]];
        return dst;
    }

    private static string EncodeRawAsPng(byte[] raw, int width, int height)
    {
        // Raw is 8-bit grayscale, row-major. Build a Bitmap with a grayscale
        // palette and copy the bytes via LockBits.
        using var bmp = new Bitmap(width, height, PixelFormat.Format8bppIndexed);
        var palette = bmp.Palette;
        for (int i = 0; i < 256; i++) palette.Entries[i] = Color.FromArgb(i, i, i);
        bmp.Palette = palette;

        var data = bmp.LockBits(
            new Rectangle(0, 0, width, height),
            ImageLockMode.WriteOnly,
            PixelFormat.Format8bppIndexed);
        try
        {
            // Stride may include padding, so copy row-by-row.
            int stride = data.Stride;
            for (int y = 0; y < height; y++)
            {
                System.Runtime.InteropServices.Marshal.Copy(
                    raw, y * width, IntPtr.Add(data.Scan0, y * stride), width);
            }
        }
        finally
        {
            bmp.UnlockBits(data);
        }

        using var ms = new MemoryStream();
        bmp.Save(ms, ImageFormat.Png);
        return Convert.ToBase64String(ms.ToArray());
    }

    private string EncodeRawAsWsq(byte[] raw, int width, int height, int dpi)
    {
        // Bitrate 1.0 ≈ 10:1 — under the 32KB API cap, no visible artifacts.
        const float bitrate = 1.0f;
        _fpm!.SetWSQImageInfo(width, height, 8, dpi);
        _fpm.SetWSQBitrate(bitrate);

        var buf = new byte[raw.Length];
        int outLen = buf.Length;
        int rc = _fpm.EncodeRawImageToWSQ(buf, ref outLen, bitrate, raw, width, height, 8, dpi, "");
        if (rc != (int)SGFPMError.ERROR_NONE)
        {
            throw new InvalidOperationException($"WSQ encode failed: {(SGFPMError)rc}");
        }
        Array.Resize(ref buf, outLen);
        return Convert.ToBase64String(buf);
    }

    private void CloseDevice()
    {
        try { _fpm?.CloseDevice(); } catch { /* swallow */ }
        _fpm = null;
        _initialised = false;
    }

    public void Dispose()
    {
        CloseDevice();
        _gate.Dispose();
    }
}
