import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
public class InspectPng {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        int w = img.getWidth(), h = img.getHeight();
        System.out.println("size " + w + "x" + h + " type=" + img.getType());
        int[] xs = {0, w-1, 0, w-1, w/2};
        int[] ys = {0, 0, h-1, h-1, h/2};
        for (int i = 0; i < xs.length; i++) System.out.printf("(%d,%d)=%08X%n", xs[i], ys[i], img.getRGB(xs[i], ys[i]));
        int minX=w, minY=h, maxX=-1, maxY=-1;
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int rgb = img.getRGB(x,y);
            int r=(rgb>>16)&255, g=(rgb>>8)&255, b=rgb&255;
            if (r+g+b < 330) { if(x<minX)minX=x; if(y<minY)minY=y; if(x>maxX)maxX=x; if(y>maxY)maxY=y; }
        }
        System.out.println("dark bbox " + minX + "," + minY + " .. " + maxX + "," + maxY);
    }
}
