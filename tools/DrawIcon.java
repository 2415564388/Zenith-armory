import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DrawIcon {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5;
                double d = Math.sqrt(dx * dx + dy * dy);
                int argb = 0;
                if (d <= 7.2) {
                    argb = 0xFF3A1255; // dark violet orb
                    if (d >= 6.0) argb = 0xFF9B4DE0; // purple rim
                    else if (x + y <= 11) argb = 0xFF552080; // top-left sheen
                }
                img.setRGB(x, y, argb);
            }
        }
        // bullet tip (copper)
        for (int y = 4; y <= 6; y++) {
            for (int x = 7; x <= 8; x++) img.setRGB(x, y, 0xFFFFB347);
        }
        // casing (gold)
        for (int y = 7; y <= 11; y++) {
            for (int x = 6; x <= 9; x++) img.setRGB(x, y, 0xFFE8C34A);
        }
        // shading on casing
        for (int y = 7; y <= 11; y++) img.setRGB(6, y, 0xFFB78E22);
        // rim
        for (int x = 5; x <= 10; x++) img.setRGB(x, 11, 0xFFC9A02E);
        // glint
        img.setRGB(5, 5, 0xFFFFFFFF);
        img.setRGB(5, 6, 0xFFE8D7FF);
        ImageIO.write(img, "png", new File(args[0]));
        System.out.println("wrote " + args[0]);
    }
}
