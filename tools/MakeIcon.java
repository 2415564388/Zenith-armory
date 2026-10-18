import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;

/** 把参考图裁成 64x64 方形物品图标，抠掉浅色背景。 */
public class MakeIcon {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        int w = src.getWidth(), h = src.getHeight();

        // 1. 从四周洪泛填充浅色背景为透明（枪械被深色垫子包围，不会被误伤）
        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = argb.createGraphics();
        g0.drawImage(src, 0, 0, null);
        g0.dispose();

        boolean[][] bg = new boolean[h][w];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            addIfLight(argb, bg, queue, x, 0);
            addIfLight(argb, bg, queue, x, h - 1);
        }
        for (int y = 0; y < h; y++) {
            addIfLight(argb, bg, queue, 0, y);
            addIfLight(argb, bg, queue, w - 1, y);
        }
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];
            argb.setRGB(x, y, 0);
            if (x > 0) addIfLight(argb, bg, queue, x - 1, y);
            if (x + 1 < w) addIfLight(argb, bg, queue, x + 1, y);
            if (y > 0) addIfLight(argb, bg, queue, x, y - 1);
            if (y + 1 < h) addIfLight(argb, bg, queue, x, y + 1);
        }

        // 2. 找剩余内容的包围盒
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((argb.getRGB(x, y) >>> 24) != 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // 3. 等比缩放并居中到 64x64
        int size = 64, pad = 2;
        int cw = maxX - minX + 1, ch = maxY - minY + 1;
        double scale = Math.min((size - pad * 2) / (double) cw, (size - pad * 2) / (double) ch);
        int dw = Math.max(1, (int) Math.round(cw * scale));
        int dh = Math.max(1, (int) Math.round(ch * scale));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(argb, (size - dw) / 2, (size - dh) / 2, dw, dh,
                minX, minY, minX + cw, minY + ch, null);
        g.dispose();
        ImageIO.write(out, "png", new File(args[1]));
        System.out.println("wrote " + args[1] + " (" + dw + "x" + dh + " content)");
    }

    private static void addIfLight(BufferedImage img, boolean[][] bg, ArrayDeque<int[]> q, int x, int y) {
        if (bg[y][x]) return;
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        if (r > 150 && g > 150 && b > 150) {
            bg[y][x] = true;
            q.add(new int[]{x, y});
        }
    }
}
