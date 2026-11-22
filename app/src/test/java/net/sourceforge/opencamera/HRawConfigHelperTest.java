package net.sourceforge.opencamera;

import net.sourceforge.opencamera.cameracontroller.CameraController;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HRawConfigHelperTest {

    @Test
    public void testGetPreviewCapHeightAndParseFps() {
        assertEquals(-1, HRawConfigHelper.getPreviewCapHeight("auto"));
        assertEquals(720, HRawConfigHelper.getPreviewCapHeight("720p"));
        assertEquals(480, HRawConfigHelper.getPreviewCapHeight("480p"));
        assertEquals(240, HRawConfigHelper.getPreviewCapHeight("240p"));
        assertEquals(480, HRawConfigHelper.getPreviewCapHeight(null));

        assertEquals(0, HRawConfigHelper.parseFpsLimitPref("default"));
        assertEquals(0, HRawConfigHelper.parseFpsLimitPref("invalid"));
        assertEquals(12, HRawConfigHelper.parseFpsLimitPref("12"));
        assertEquals(8, HRawConfigHelper.parseFpsLimitPref("8"));
    }

    @Test
    public void testSelectPreviewSizeWithCap() {
        List<CameraController.Size> sizes = Arrays.asList(
                new CameraController.Size(1920, 1440),
                new CameraController.Size(1440, 1080),
                new CameraController.Size(1280, 720),
                new CameraController.Size(960, 720),
                new CameraController.Size(800, 480),
                new CameraController.Size(640, 480),
                new CameraController.Size(320, 240)
        );
        double target43 = 4.0 / 3.0;

        CameraController.Size cap480 = HRawConfigHelper.selectPreviewSizeWithCap(sizes, target43, 480);
        assertNotNull(cap480);
        assertEquals(640, cap480.width);
        assertEquals(480, cap480.height);

        CameraController.Size cap720 = HRawConfigHelper.selectPreviewSizeWithCap(sizes, target43, 720);
        assertNotNull(cap720);
        assertEquals(960, cap720.width);
        assertEquals(720, cap720.height);

        CameraController.Size cap240 = HRawConfigHelper.selectPreviewSizeWithCap(sizes, target43, 240);
        assertNotNull(cap240);
        assertEquals(320, cap240.width);
        assertEquals(240, cap240.height);

        assertNull(HRawConfigHelper.selectPreviewSizeWithCap(sizes, target43, -1));
    }

    @Test
    public void testComputeManualFrameDurationNs() {
        long maxA53 = 142_857_142L;
        assertEquals(0L, HRawConfigHelper.computeManualFrameDurationNs(0, 12_500_000L, maxA53));
        assertEquals(83_333_333L, HRawConfigHelper.computeManualFrameDurationNs(12, 12_500_000L, maxA53));
        // Exposure longer than 1/12s must win
        assertEquals(100_000_000L, HRawConfigHelper.computeManualFrameDurationNs(12, 100_000_000L, maxA53));
        // Very low FPS clamped to hardware maxFrameDuration
        assertEquals(maxA53, HRawConfigHelper.computeManualFrameDurationNs(5, 12_500_000L, maxA53));
    }

    @Test
    public void testSelectBestLowFpsAeRange() {
        List<int[]> a53Ranges = Arrays.asList(
                new int[]{15, 15},
                new int[]{15, 20},
                new int[]{20, 20},
                new int[]{24, 24},
                new int[]{8, 30},
                new int[]{10, 30},
                new int[]{15, 30},
                new int[]{30, 30}
        );
        assertArrayEquals(new int[]{15, 15}, HRawConfigHelper.selectBestLowFpsAeRange(a53Ranges, 12));
        assertArrayEquals(new int[]{15, 15}, HRawConfigHelper.selectBestLowFpsAeRange(a53Ranges, 15));

        List<int[]> lowerRanges = Arrays.asList(
                new int[]{10, 10},
                new int[]{15, 15},
                new int[]{15, 30}
        );
        assertArrayEquals(new int[]{10, 10}, HRawConfigHelper.selectBestLowFpsAeRange(lowerRanges, 12));
        assertNull(HRawConfigHelper.selectBestLowFpsAeRange(Collections.emptyList(), 12));
    }

    @Test
    public void testStepIsoAndShutter() {
        assertEquals(1000, HRawConfigHelper.stepIso(800, 1, 50, 3200));
        assertEquals(640, HRawConfigHelper.stepIso(800, -1, 50, 3200));
        // Near-stop snap (798 treated as 800 -> +1 steps to 1000)
        assertEquals(1000, HRawConfigHelper.stepIso(798, 1, 50, 3200));
        // Clamp bounds
        assertEquals(3200, HRawConfigHelper.stepIso(3200, 1, 50, 3200));
        assertEquals(50, HRawConfigHelper.stepIso(50, -1, 50, 3200));

        long minNs = 42_000L;
        long maxNs = 100_000_000L;
        assertEquals(16_666_667L, HRawConfigHelper.stepShutterNs(12_500_000L, 1, minNs, maxNs));
        assertEquals(10_000_000L, HRawConfigHelper.stepShutterNs(12_500_000L, -1, minNs, maxNs));
        assertEquals(maxNs, HRawConfigHelper.stepShutterNs(maxNs, 1, minNs, maxNs));
        assertEquals(minNs, HRawConfigHelper.stepShutterNs(minNs, -1, minNs, maxNs));
    }

    @Test
    public void testStorageAndFormatting() {
        long bytesPerShot = HRawConfigHelper.estimateBytesPerShot(4624, 3468, true);
        assertEquals(4624L * 3468L * 2L + 4L * 1024L * 1024L, bytesPerShot);
        long shots = HRawConfigHelper.estimateRemainingRawShots(10L * 1024L * 1024L * 1024L, bytesPerShot);
        assertEquals((10L * 1024L * 1024L * 1024L) / bytesPerShot, shots);

        assertEquals("--", HRawConfigHelper.formatBriefShutter(0));
        assertEquals("1/80s", HRawConfigHelper.formatBriefShutter(12_500_000L));
        assertEquals("1/15s", HRawConfigHelper.formatBriefShutter(66_666_667L));
        assertEquals("1s", HRawConfigHelper.formatBriefShutter(1_000_000_000L));
        assertEquals("1.5s", HRawConfigHelper.formatBriefShutter(1_500_000_000L));
    }

    @Test
    public void testComputeMeteringProbeDelayMs() {
        assertEquals(HRawConfigHelper.METERING_PROBE_FAST_MS,
                HRawConfigHelper.computeMeteringProbeDelayMs(true, 800, 800, 12_500_000L, 12_500_000L));
        assertEquals(HRawConfigHelper.METERING_PROBE_FAST_MS,
                HRawConfigHelper.computeMeteringProbeDelayMs(false, 0, 800, 12_500_000L, 12_500_000L));
        assertEquals(HRawConfigHelper.METERING_PROBE_FAST_MS,
                HRawConfigHelper.computeMeteringProbeDelayMs(false, 800, 0, 12_500_000L, 12_500_000L));
        assertEquals(HRawConfigHelper.METERING_PROBE_FAST_MS,
                HRawConfigHelper.computeMeteringProbeDelayMs(false, 400, 500, 12_500_000L, 12_500_000L));
        assertEquals(HRawConfigHelper.METERING_PROBE_FAST_MS,
                HRawConfigHelper.computeMeteringProbeDelayMs(false, 800, 800, 10_000_000L, 12_500_000L));
        assertEquals(HRawConfigHelper.METERING_PROBE_STEADY_MS,
                HRawConfigHelper.computeMeteringProbeDelayMs(false, 800, 820, 12_500_000L, 12_800_000L));
    }
}
