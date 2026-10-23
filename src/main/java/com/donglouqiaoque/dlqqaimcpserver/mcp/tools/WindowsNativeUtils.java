package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HCURSOR;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.POINT;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Windows 原生窗口相关工具（通过 JNA 调用 user32 / gdi32）。
 *
 * <p>提供两类能力：</p>
 * <ol>
 *   <li>{@link #getCursorPos()}：获取鼠标光标当前位置（GetCursorPos，作为 Java MouseInfo 的补充，
 *       在 MouseInfo 返回 null 的环境下更可靠）；</li>
 *   <li>{@link #drawCursor(BufferedImage, Rectangle)}：获取当前鼠标光标位图并绘制到截图上。
 *       java.awt.Robot.createScreenCapture 截取的图像不包含鼠标光标，
 *       这里通过 GetCursorInfo + GetIconInfo + GetDIBits 拿到真实光标位图（含热点、透明通道），
 *       再按 alpha 混合绘制到截图对应位置。</li>
 * </ol>
 *
 * <p>仅在 Windows 平台生效；其他平台或任何原生调用失败时静默降级（不影响截图/坐标主流程）。</p>
 */
final class WindowsNativeUtils {

    /** 是否为 Windows 平台 */
    private static final boolean IS_WINDOWS;

    static {
        String os = System.getProperty("os.name", "");
        IS_WINDOWS = os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }

    private WindowsNativeUtils() {
    }

    /** 是否输出诊断日志到 stderr（用于排查光标绘制问题） */
    private static boolean debugLog(String msg) {
        try {
            System.err.println("[CursorDebug] " + msg);
        } catch (Throwable ignored) {
        }
        return false;
    }

    // ==================== user32 ====================

    public interface IUser32 extends Library {
        IUser32 INSTANCE = Native.load("user32", IUser32.class);

        /** 获取光标信息（位置 + 光标句柄） */
        boolean GetCursorInfo(CURSORINFO pci);

        /** 获取光标图标信息（热点、颜色位图、掩码位图） */
        boolean GetIconInfo(HCURSOR hIcon, ICONINFO pIconInfo);

        /** 获取屏幕 DC */
        HDC GetDC(Pointer hWnd);

        /** 释放 DC */
        int ReleaseDC(Pointer hWnd, HDC hDC);

        /** 获取光标位置 */
        boolean GetCursorPos(POINT lpPoint);
    }

    // ==================== gdi32 ====================

    public interface IGdi32 extends Library {
        IGdi32 INSTANCE = Native.load("gdi32", IGdi32.class);

        HDC CreateCompatibleDC(HDC hdc);

        boolean DeleteDC(HDC hdc);

        HANDLE SelectObject(HDC hdc, HANDLE h);

        boolean DeleteObject(HANDLE h);

        /** 获取位图 DIB 数据（DIB_RGB_COLORS = 0） */
        int GetDIBits(HDC hdc, HANDLE hbm, int start, int cLines, Pointer lpvBits, BITMAPINFO lpbmi, int usage);

        /** 获取对象信息（用于读取位图尺寸） */
        int GetObjectW(HANDLE h, int c, Pointer pv);
    }

    // ==================== 结构体定义 ====================

    /** CURSORINFO - 光标信息 */
    @Structure.FieldOrder({"cbSize", "flags", "hCursor", "ptScreenPos"})
    public static class CURSORINFO extends Structure {
        public int cbSize;
        public int flags;
        public HCURSOR hCursor;
        public POINT ptScreenPos = new POINT();
    }

    /** ICONINFO - 图标信息 */
    @Structure.FieldOrder({"fIcon", "xHotspot", "yHotspot", "hbmMask", "hbmColor"})
    public static class ICONINFO extends Structure {
        public boolean fIcon;
        public int xHotspot;
        public int yHotspot;
        public HBITMAP hbmMask;
        public HBITMAP hbmColor;
    }

    /** BITMAPINFOHEADER */
    @Structure.FieldOrder({"biSize", "biWidth", "biHeight", "biPlanes", "biBitCount", "biCompression",
            "biSizeImage", "biXPelsPerMeter", "biYPelsPerMeter", "biClrUsed", "biClrImportant"})
    public static class BITMAPINFOHEADER extends Structure {
        public int biSize;
        public int biWidth;
        public int biHeight;
        public short biPlanes;
        public short biBitCount;
        public int biCompression;
        public int biSizeImage;
        public int biXPelsPerMeter;
        public int biYPelsPerMeter;
        public int biClrUsed;
        public int biClrImportant;
    }

    /** RGBQUAD */
    @Structure.FieldOrder({"rgbBlue", "rgbGreen", "rgbRed", "rgbReserved"})
    public static class RGBQUAD extends Structure {
        public byte rgbBlue;
        public byte rgbGreen;
        public byte rgbRed;
        public byte rgbReserved;
    }

    /** BITMAPINFO - 头部 + 调色板 */
    public static class BITMAPINFO extends Structure {
        public BITMAPINFOHEADER bmiHeader = new BITMAPINFOHEADER();
        public RGBQUAD[] bmiColors;

        public BITMAPINFO(int paletteSize) {
            bmiColors = new RGBQUAD[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                bmiColors[i] = new RGBQUAD();
            }
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("bmiHeader", "bmiColors");
        }
    }

    /** BITMAP - 位图基本信息（GetObjectW 用） */
    @Structure.FieldOrder({"bmType", "bmWidth", "bmHeight", "bmWidthBytes", "bmPlanes", "bmBitsPixel", "bmBits"})
    public static class BITMAP extends Structure {
        public int bmType;
        public int bmWidth;
        public int bmHeight;
        public int bmWidthBytes;
        public short bmPlanes;
        public short bmBitsPixel;
        public Pointer bmBits;
    }

    // ==================== 光标位图数据 ====================

    /**
     * 光标位图数据。
     * 颜色模式：{@link #argb} 有效，每像素 0xAARRGGBB（B、G、R、A 字节序与 int 小端一致）。
     * 掩码模式：{@link #maskBits} 有效，1bpp，每行按 4 字节对齐（stride）。
     */
    private static final class CursorBitmap {
        final int width;
        final int height;      // 位图总高度（单色光标的掩码位图为 2*图标高度：上半 AND 平面 + 下半 XOR 平面）
        final int[] argb;      // 颜色位图
        final byte[] maskBits; // 掩码位图
        final int maskStride;

        CursorBitmap(int width, int height, int[] argb, byte[] maskBits, int maskStride) {
            this.width = width;
            this.height = height;
            this.argb = argb;
            this.maskBits = maskBits;
            this.maskStride = maskStride;
        }

        /** 掩码位（1bpp，x=0 为字节最高位）。bit=1 表示透明（AND 掩码中白色=1=透明）。 */
        boolean maskBit(int x, int y) {
            int idx = y * maskStride + (x >> 3);
            int bit = 0x80 >> (x & 7);
            return (maskBits[idx] & bit) != 0;
        }

        /** 是否存在任何非 0 的 alpha（用于判断是否为 alpha 通道光标） */
        boolean hasAlpha() {
            if (argb == null) return false;
            for (int a : argb) {
                if ((a & 0xFF000000) != 0) return true;
            }
            return false;
        }
    }

    // ==================== 对外方法 ====================

    /**
     * 获取鼠标光标当前位置。
     *
     * @return 光标位置；失败返回 null
     */
    static Point getCursorPos() {
        if (!IS_WINDOWS) return null;
        try {
            POINT p = new POINT();
            if (IUser32.INSTANCE.GetCursorPos(p)) {
                return new Point(p.x, p.y);
            }
        } catch (Throwable t) {
            // 忽略，调用方自行降级
        }
        return null;
    }

    /**
     * 将当前鼠标光标绘制到截图图像上。
     *
     * @param image       截图图像（像素将被就地修改）
     * @param captureRect 截取区域（屏幕坐标），用于把光标的屏幕坐标换算成图像坐标
     * @return true 表示成功绘制了光标；false 表示未绘制（非 Windows、光标隐藏、或原生调用失败）
     */
    static boolean drawCursor(BufferedImage image, Rectangle captureRect) {
        if (!IS_WINDOWS || image == null || captureRect == null) {
            return debugLog("跳过: IS_WINDOWS=" + IS_WINDOWS + ", image=" + (image != null) + ", captureRect=" + captureRect);
        }
        try {
            CURSORINFO ci = new CURSORINFO();
            ci.cbSize = ci.size();
            if (!IUser32.INSTANCE.GetCursorInfo(ci)) {
                return debugLog("GetCursorInfo 失败");
            }
            if (ci.hCursor == null) {
                // 光标被隐藏（如全屏视频播放时），无需绘制
                return debugLog("光标被隐藏 (hCursor=null)");
            }

            ICONINFO ii = new ICONINFO();
            if (!IUser32.INSTANCE.GetIconInfo(ci.hCursor, ii)) {
                return debugLog("GetIconInfo 失败");
            }
            try {
                CursorBitmap color = ii.hbmColor != null ? readColorBitmap(ii.hbmColor) : null;
                CursorBitmap mask = ii.hbmMask != null ? readMaskBitmap(ii.hbmMask) : null;
                if (color == null && mask == null) {
                    return debugLog("color/mask 均为 null");
                }
                debugLog("OK: color=" + (color != null ? color.width + "x" + color.height : "null")
                        + ", mask=" + (mask != null ? mask.width + "x" + mask.height : "null"));

                int cursorWidth = color != null ? color.width : mask.width;
                // 颜色光标的掩码高度 = 图标高度；单色光标的掩码高度 = 2*图标高度
                int cursorHeight = color != null ? color.height : mask.height / 2;
                boolean alphaMode = color != null && color.hasAlpha();
                boolean mono = color == null;
                if (cursorWidth <= 0 || cursorHeight <= 0) {
                    return false;
                }

                // 热点屏幕坐标优先使用 GetCursorPos（与 Robot 截图坐标空间一致，含 DPI 虚拟化），
                // 失败时退回 GetCursorInfo 返回的屏幕坐标
                Point hot = getCursorPos();
                int hotX = hot != null ? hot.x : ci.ptScreenPos.x;
                int hotY = hot != null ? hot.y : ci.ptScreenPos.y;

                // 光标图像左上角在截图图像中的坐标 = 热点屏幕坐标 - 截取区域原点
                int drawX = hotX - ii.xHotspot - captureRect.x;
                int drawY = hotY - ii.yHotspot - captureRect.y;

                int imgW = image.getWidth();
                int imgH = image.getHeight();
                boolean drawn = false;

                for (int cy = 0; cy < cursorHeight; cy++) {
                    int py = drawY + cy;
                    if (py < 0 || py >= imgH) continue;
                    for (int cx = 0; cx < cursorWidth; cx++) {
                        int px = drawX + cx;
                        if (px < 0 || px >= imgW) continue;

                        boolean maskTransparent = mask != null && mask.maskBit(cx, cy);
                        int srcRgb;
                        int srcAlpha;

                        if (alphaMode) {
                            // 32bpp alpha 光标：直接用 alpha 混合，alpha=0 时参考 AND 掩码
                            int a = color.argb[cy * color.width + cx];
                            srcAlpha = (a >>> 24) & 0xFF;
                            srcRgb = a & 0xFFFFFF;
                            if (srcAlpha == 0) {
                                if (maskTransparent) continue;
                                srcAlpha = 255;
                            }
                        } else if (!mono) {
                            // 颜色位图无 alpha：用 AND 掩码决定透明，颜色直接取颜色位图
                            if (maskTransparent) continue;
                            srcRgb = color.argb[cy * color.width + cx] & 0xFFFFFF;
                            srcAlpha = 255;
                        } else {
                            // 单色光标：AND 掩码决定透明，XOR 掩码决定黑白
                            if (maskTransparent) continue;
                            boolean xorBit = mask.maskBit(cx, cy + cursorHeight);
                            srcRgb = xorBit ? 0xFFFFFF : 0x000000;
                            srcAlpha = 255;
                        }
                        if (srcAlpha == 0) continue;

                        int dst = image.getRGB(px, py);
                        int sr = (srcRgb >> 16) & 0xFF;
                        int sg = (srcRgb >> 8) & 0xFF;
                        int sb = srcRgb & 0xFF;
                        int dr = (dst >> 16) & 0xFF;
                        int dg = (dst >> 8) & 0xFF;
                        int db = dst & 0xFF;
                        int nr = (sr * srcAlpha + dr * (255 - srcAlpha)) / 255;
                        int ng = (sg * srcAlpha + dg * (255 - srcAlpha)) / 255;
                        int nb = (sb * srcAlpha + db * (255 - srcAlpha)) / 255;
                        image.setRGB(px, py, (dst & 0xFF000000) | (nr << 16) | (ng << 8) | nb);
                        drawn = true;
                    }
                }
                debugLog("结束: drawn=" + drawn + ", hot=(" + hotX + "," + hotY + "), hotspot=(" + ii.xHotspot + "," + ii.yHotspot
                        + "), draw=(" + drawX + "," + drawY + "), img=" + imgW + "x" + imgH
                        + ", cursor=" + cursorWidth + "x" + cursorHeight + ", alphaMode=" + alphaMode + ", mono=" + mono);
                return drawn;
            } finally {
                // GetIconInfo 创建的两个位图必须释放
                if (ii.hbmColor != null) {
                    IGdi32.INSTANCE.DeleteObject(ii.hbmColor);
                }
                if (ii.hbmMask != null) {
                    IGdi32.INSTANCE.DeleteObject(ii.hbmMask);
                }
            }
        } catch (Throwable t) {
            // 光标绘制失败不影响截图本身，记录原因便于排查
            return debugLog("异常: " + t);
        }
    }

    // ==================== 位图读取 ====================

    /** 以 32bpp 读取颜色位图（GetDIBits 会把任意色深转换为 32bpp BGRA） */
    private static CursorBitmap readColorBitmap(HBITMAP hbm) {
        int[] size = bitmapSize(hbm);
        if (size == null) return null;
        int w = size[0], h = size[1];

        HDC screenDC = IUser32.INSTANCE.GetDC(null);
        HDC memDC = IGdi32.INSTANCE.CreateCompatibleDC(screenDC);
        if (memDC == null) {
            IUser32.INSTANCE.ReleaseDC(null, screenDC);
            return null;
        }
        try {
            HANDLE old = IGdi32.INSTANCE.SelectObject(memDC, hbm);
            try {
                BITMAPINFO bmi = new BITMAPINFO(1);
                bmi.bmiHeader.biSize = bmi.bmiHeader.size();
                bmi.bmiHeader.biWidth = w;
                bmi.bmiHeader.biHeight = -h;      // 负值：自上而下返回
                bmi.bmiHeader.biPlanes = 1;
                bmi.bmiHeader.biBitCount = 32;
                bmi.bmiHeader.biCompression = 0;  // BI_RGB

                Memory buf = new Memory((long) w * h * 4);
                int lines = IGdi32.INSTANCE.GetDIBits(memDC, hbm, 0, h, buf, bmi, 0);
                if (lines != h) return null;
                int[] argb = new int[w * h];
                buf.read(0, argb, 0, argb.length);
                return new CursorBitmap(w, h, argb, null, 0);
            } finally {
                IGdi32.INSTANCE.SelectObject(memDC, old);
            }
        } finally {
            IGdi32.INSTANCE.DeleteDC(memDC);
            IUser32.INSTANCE.ReleaseDC(null, screenDC);
        }
    }

    /** 以 1bpp 读取掩码位图（AND 平面，单色光标时还包含下半部分的 XOR 平面） */
    private static CursorBitmap readMaskBitmap(HBITMAP hbm) {
        int[] size = bitmapSize(hbm);
        if (size == null) return null;
        int w = size[0], h = size[1];

        HDC screenDC = IUser32.INSTANCE.GetDC(null);
        HDC memDC = IGdi32.INSTANCE.CreateCompatibleDC(screenDC);
        if (memDC == null) {
            IUser32.INSTANCE.ReleaseDC(null, screenDC);
            return null;
        }
        try {
            HANDLE old = IGdi32.INSTANCE.SelectObject(memDC, hbm);
            try {
                int stride = ((w + 31) / 32) * 4;
                BITMAPINFO bmi = new BITMAPINFO(2);
                bmi.bmiHeader.biSize = bmi.bmiHeader.size();
                bmi.bmiHeader.biWidth = w;
                bmi.bmiHeader.biHeight = -h;      // 负值：自上而下返回
                bmi.bmiHeader.biPlanes = 1;
                bmi.bmiHeader.biBitCount = 1;
                bmi.bmiHeader.biCompression = 0;  // BI_RGB
                bmi.bmiHeader.biClrUsed = 2;

                Memory buf = new Memory((long) stride * h);
                int lines = IGdi32.INSTANCE.GetDIBits(memDC, hbm, 0, h, buf, bmi, 0);
                if (lines != h) return null;
                byte[] bits = new byte[stride * h];
                buf.read(0, bits, 0, bits.length);
                return new CursorBitmap(w, h, null, bits, stride);
            } finally {
                IGdi32.INSTANCE.SelectObject(memDC, old);
            }
        } finally {
            IGdi32.INSTANCE.DeleteDC(memDC);
            IUser32.INSTANCE.ReleaseDC(null, screenDC);
        }
    }

    /** 读取位图宽高，返回 {width, height}；失败返回 null */
    private static int[] bitmapSize(HBITMAP hbm) {
        try {
            BITMAP bm = new BITMAP();
            if (IGdi32.INSTANCE.GetObjectW(hbm, bm.size(), bm.getPointer()) == 0) {
                return null;
            }
            // 通过 getPointer() 传递的结构体不会被 JNA 自动回读，需手动 read() 同步原生数据
            bm.read();
            int w = bm.bmWidth, h = bm.bmHeight;
            if (w <= 0 || h <= 0 || w > 1024 || h > 2048) {
                return null;
            }
            return new int[]{w, h};
        } catch (Throwable t) {
            return null;
        }
    }
}
