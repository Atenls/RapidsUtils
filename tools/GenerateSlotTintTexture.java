import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Deterministic grayscale mask, tinted at render time. Requires only the Java 21 standard library. */
public class GenerateSlotTintTexture {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        BufferedImage center = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                double radius = Math.hypot((x + 0.5 - 16) / 16, (y + 0.5 - 16) / 16);
                double coverage = Math.max(0, Math.min(1, (1 - radius) * 16 + 0.5));
                double fade = Math.max(0, Math.min(1, (radius - 0.08) / 0.92));
                int alpha = (int) Math.round(255 * coverage);
                int centerAlpha = (int) Math.round(alpha * (1 - fade * fade * (3 - 2 * fade)));
                int shade = (int) Math.round(184 - 48 * Math.min(1, radius));
                image.setRGB(x, y, (alpha << 24) | (shade << 16) | (shade << 8) | shade);
                center.setRGB(x, y, (centerAlpha << 24) | (shade << 16) | (shade << 8) | shade);
            }
        }
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
        ImageIO.write(center, "png", output.resolveSibling("item_slot_tint_center.png").toFile());
    }
}
