package net.sourceforge.opencamera;

import net.sourceforge.opencamera.cameracontroller.CameraController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper utilities for HRaw defaults, thermal preview caps, photographic stop stepping,
 * church low-light manual presets, and RAW storage estimation.
 */
public final class HRawConfigHelper {
    public static final int PRESET_PALCO_ISO = 800;
    public static final long PRESET_PALCO_SHUTTER_NS = 12_500_000L; // 1/80 s

    public static final int PRESET_ESCURO_ISO = 1600;
    public static final long PRESET_ESCURO_SHUTTER_NS = 20_000_000L; // 1/50 s

    public static final int PRESET_ESTATICO_ISO = 400;
    public static final long PRESET_ESTATICO_SHUTTER_NS = 66_666_667L; // 1/15 s

    public static final long DEFAULT_RAW_BYTES = 32L * 1024L * 1024L;
    public static final long DEFAULT_JPEG_BYTES = 4L * 1024L * 1024L;

    public static final long METERING_PROBE_FAST_MS = 250L;
    public static final long METERING_PROBE_STEADY_MS = 1000L;

    public static final int[] ISO_STOPS = {
            50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640,
            800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400
    };

    public static final long[] SHUTTER_STOPS_NS = {
            125_000L,       // 1/8000s
            250_000L,       // 1/4000s
            500_000L,       // 1/2000s
            1_000_000L,     // 1/1000s
            2_000_000L,     // 1/500s
            4_000_000L,     // 1/250s
            5_000_000L,     // 1/200s
            6_250_000L,     // 1/160s
            8_000_000L,     // 1/125s
            10_000_000L,    // 1/100s
            12_500_000L,    // 1/80s
            16_666_667L,    // 1/60s
            20_000_000L,    // 1/50s
            25_000_000L,    // 1/40s
            33_333_333L,    // 1/30s
            40_000_000L,    // 1/25s
            50_000_000L,    // 1/20s
            66_666_667L,    // 1/15s
            100_000_000L,   // 1/10s
            125_000_000L,   // 1/8s
            250_000_000L,   // 1/4s
            500_000_000L,   // 1/2s
            1_000_000_000L, // 1s
            2_000_000_000L, // 2s
            4_000_000_000L  // 4s
    };

    private static final double ASPECT_TOLERANCE = 0.05;
    private static final double SNAP_TOLERANCE_RATIO = 0.03;

    private HRawConfigHelper() {
    }

    /**
     * Converts preview quality preference into maximum short-side pixel height, or <= 0 if uncapped.
     */
    public static int getPreviewCapHeight(String pref) {
        if (pref == null) {
            return 480;
        }
        switch (pref) {
            case "auto":
                return -1;
            case "720p":
                return 720;
            case "240p":
                return 240;
            case "480p":
            default:
                return 480;
        }
    }

    /**
     * Parses FPS limit preference into target FPS, or 0 for default uncapped preview FPS.
     */
    public static int parseFpsLimitPref(String pref) {
        if (pref == null || pref.isEmpty() || "default".equals(pref)) {
            return 0;
        }
        try {
            int val = Integer.parseInt(pref);
            return Math.max(0, val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Selects the largest preview size with minor dimension <= maxHeight matching targetRatio
     * (within tolerance), or closest aspect ratio fallback. Returns null when uncapped or empty.
     */
    public static CameraController.Size selectPreviewSizeWithCap(
            List<CameraController.Size> sizes,
            double targetRatio,
            int maxHeight) {
        if (maxHeight <= 0 || sizes == null || sizes.isEmpty()) {
            return null;
        }
        List<CameraController.Size> capped = new ArrayList<>();
        for (CameraController.Size size : sizes) {
            int minor = Math.min(size.width, size.height);
            if (minor <= maxHeight) {
                capped.add(size);
            }
        }
        List<CameraController.Size> pool = capped.isEmpty() ? sizes : capped;

        CameraController.Size bestMatchingRatio = null;
        for (CameraController.Size size : pool) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) <= ASPECT_TOLERANCE) {
                if (bestMatchingRatio == null || isLarger(size, bestMatchingRatio, capped.isEmpty())) {
                    bestMatchingRatio = size;
                }
            }
        }
        if (bestMatchingRatio != null) {
            return bestMatchingRatio;
        }

        CameraController.Size closest = null;
        double minDiff = Double.MAX_VALUE;
        for (CameraController.Size size : pool) {
            double ratio = (double) size.width / size.height;
            double diff = Math.abs(ratio - targetRatio);
            if (closest == null || diff < minDiff - 1e-4
                    || (Math.abs(diff - minDiff) <= 1e-4 && isLarger(size, closest, capped.isEmpty()))) {
                closest = size;
                minDiff = diff;
            }
        }
        return closest;
    }

    private static boolean isLarger(CameraController.Size candidate, CameraController.Size current, boolean preferSmaller) {
        long candArea = (long) candidate.width * candidate.height;
        long currArea = (long) current.width * current.height;
        return preferSmaller ? (candArea < currArea) : (candArea > currArea);
    }

    /**
     * Computes SENSOR_FRAME_DURATION in nanoseconds for Manual mode (AE OFF) to enforce an FPS ceiling
     * while honoring exposureTimeNs and hardware maxFrameDurationNs. Returns 0 when fpsLimit <= 0.
     */
    public static long computeManualFrameDurationNs(int fpsLimit, long exposureTimeNs, long maxFrameDurationNs) {
        if (fpsLimit <= 0) {
            return 0L;
        }
        long targetNs = 1_000_000_000L / fpsLimit;
        if (maxFrameDurationNs > 0 && targetNs > maxFrameDurationNs) {
            targetNs = maxFrameDurationNs;
        }
        return Math.max(targetNs, exposureTimeNs);
    }

    /**
     * Selects the AE target FPS range closest to fpsLimit (preferring max <= fpsLimit, otherwise smallest
     * available max FPS ceiling). Ties prefer narrower [min, max] range and higher max.
     */
    public static int[] selectBestLowFpsAeRange(List<int[]> supportedRanges, int fpsLimit) {
        if (fpsLimit <= 0 || supportedRanges == null || supportedRanges.isEmpty()) {
            return null;
        }
        int[] bestUnderCap = null;
        for (int[] range : supportedRanges) {
            if (range == null || range.length < 2) {
                continue;
            }
            if (range[1] <= fpsLimit) {
                if (bestUnderCap == null
                        || range[1] > bestUnderCap[1]
                        || (range[1] == bestUnderCap[1] && (range[1] - range[0]) < (bestUnderCap[1] - bestUnderCap[0]))) {
                    bestUnderCap = range;
                }
            }
        }
        if (bestUnderCap != null) {
            return bestUnderCap;
        }

        int[] lowestCeiling = null;
        for (int[] range : supportedRanges) {
            if (range == null || range.length < 2) {
                continue;
            }
            if (lowestCeiling == null
                    || range[1] < lowestCeiling[1]
                    || (range[1] == lowestCeiling[1] && (range[1] - range[0]) < (lowestCeiling[1] - lowestCeiling[0]))) {
                lowestCeiling = range;
            }
        }
        return lowestCeiling;
    }

    /**
     * Steps ISO along photographic stops (direction > 0 increases ISO; direction < 0 decreases ISO)
     * clamped to [minIso, maxIso].
     */
    public static int stepIso(int currentIso, int direction, int minIso, int maxIso) {
        if (minIso > maxIso) {
            return currentIso;
        }
        int clamped = Math.max(minIso, Math.min(maxIso, currentIso));
        if (direction == 0) {
            return clamped;
        }
        int snapIndex = findSnapIndexInt(ISO_STOPS, clamped);
        if (snapIndex >= 0) {
            int nextIndex = snapIndex + (direction > 0 ? 1 : -1);
            while (nextIndex >= 0 && nextIndex < ISO_STOPS.length) {
                int candidate = ISO_STOPS[nextIndex];
                if (candidate >= minIso && candidate <= maxIso && candidate != clamped) {
                    return candidate;
                }
                nextIndex += (direction > 0 ? 1 : -1);
            }
            return direction > 0 ? maxIso : minIso;
        }
        if (direction > 0) {
            for (int stop : ISO_STOPS) {
                if (stop > clamped && stop >= minIso && stop <= maxIso) {
                    return stop;
                }
            }
            return maxIso;
        } else {
            for (int i = ISO_STOPS.length - 1; i >= 0; i--) {
                int stop = ISO_STOPS[i];
                if (stop < clamped && stop >= minIso && stop <= maxIso) {
                    return stop;
                }
            }
            return minIso;
        }
    }

    /**
     * Steps shutter exposure duration (ns) along photographic stops (direction > 0 lengthens exposure/brighter;
     * direction < 0 shortens exposure/faster) clamped to [minNs, maxNs].
     */
    public static long stepShutterNs(long currentNs, int direction, long minNs, long maxNs) {
        if (minNs > maxNs) {
            return currentNs;
        }
        long clamped = Math.max(minNs, Math.min(maxNs, currentNs));
        if (direction == 0) {
            return clamped;
        }
        int snapIndex = findSnapIndexLong(SHUTTER_STOPS_NS, clamped);
        if (snapIndex >= 0) {
            int nextIndex = snapIndex + (direction > 0 ? 1 : -1);
            while (nextIndex >= 0 && nextIndex < SHUTTER_STOPS_NS.length) {
                long candidate = SHUTTER_STOPS_NS[nextIndex];
                if (candidate >= minNs && candidate <= maxNs && candidate != clamped) {
                    return candidate;
                }
                nextIndex += (direction > 0 ? 1 : -1);
            }
            return direction > 0 ? maxNs : minNs;
        }
        if (direction > 0) {
            for (long stop : SHUTTER_STOPS_NS) {
                if (stop > clamped && stop >= minNs && stop <= maxNs) {
                    return stop;
                }
            }
            return maxNs;
        } else {
            for (int i = SHUTTER_STOPS_NS.length - 1; i >= 0; i--) {
                long stop = SHUTTER_STOPS_NS[i];
                if (stop < clamped && stop >= minNs && stop <= maxNs) {
                    return stop;
                }
            }
            return minNs;
        }
    }

    private static int findSnapIndexInt(int[] stops, int value) {
        int best = -1;
        double bestRatioDiff = Double.MAX_VALUE;
        for (int i = 0; i < stops.length; i++) {
            double diff = Math.abs((double) stops[i] - value) / Math.max(1.0, value);
            if (diff <= SNAP_TOLERANCE_RATIO && diff < bestRatioDiff) {
                best = i;
                bestRatioDiff = diff;
            }
        }
        return best;
    }

    private static int findSnapIndexLong(long[] stops, long value) {
        int best = -1;
        double bestRatioDiff = Double.MAX_VALUE;
        for (int i = 0; i < stops.length; i++) {
            double diff = Math.abs((double) stops[i] - value) / Math.max(1.0, value);
            if (diff <= SNAP_TOLERANCE_RATIO && diff < bestRatioDiff) {
                best = i;
                bestRatioDiff = diff;
            }
        }
        return best;
    }

    /**
     * Estimates average storage size in bytes for one RAW capture (plus JPEG if enabled).
     */
    public static long estimateBytesPerShot(int rawWidth, int rawHeight, boolean includeJpeg) {
        long rawBytes = (rawWidth > 0 && rawHeight > 0)
                ? (long) rawWidth * rawHeight * 2L
                : DEFAULT_RAW_BYTES;
        return rawBytes + (includeJpeg ? DEFAULT_JPEG_BYTES : 0L);
    }

    /**
     * Estimates how many RAW captures fit in availableBytes.
     */
    public static long estimateRemainingRawShots(long availableBytes, long bytesPerShot) {
        if (availableBytes <= 0) {
            return 0L;
        }
        return availableBytes / Math.max(1L, bytesPerShot);
    }

    /**
     * Formats shutter exposure duration concisely (e.g., "1/80s", "1.0s", "--").
     */
    public static String formatBriefShutter(long exposureTimeNs) {
        if (exposureTimeNs <= 0) {
            return "--";
        }
        if (exposureTimeNs < 1_000_000_000L) {
            long denom = Math.max(1L, Math.round(1_000_000_000.0 / exposureTimeNs));
            return "1/" + denom + "s";
        }
        double seconds = exposureTimeNs / 1_000_000_000.0;
        long rounded = Math.round(seconds);
        if (Math.abs(seconds - rounded) < 0.05) {
            return rounded + "s";
        }
        return String.format(Locale.US, "%.1fs", seconds);
    }

    /**
     * Computes adaptive delay (ms) between isolated AE metering probes when preview repeating is paused.
     */
    public static long computeMeteringProbeDelayMs(
            boolean aeSearching,
            int prevIso,
            int currIso,
            long prevExposureNs,
            long currExposureNs) {
        if (aeSearching || currIso <= 0 || currExposureNs <= 0L || prevIso <= 0 || prevExposureNs <= 0L) {
            return METERING_PROBE_FAST_MS;
        }
        double isoDelta = Math.abs((double) currIso - prevIso) / Math.max(1.0, prevIso);
        double expDelta = Math.abs((double) currExposureNs - prevExposureNs) / Math.max(1.0, prevExposureNs);
        if (isoDelta > 0.08 || expDelta > 0.08) {
            return METERING_PROBE_FAST_MS;
        }
        return METERING_PROBE_STEADY_MS;
    }
}
