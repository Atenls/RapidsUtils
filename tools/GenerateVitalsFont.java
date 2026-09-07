import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Original 5x7 pixel glyphs, licensed under the repository's MIT license.
 * Run from the repository root: java tools/GenerateVitalsFont.java
 * Each two-digit hex number is one row, with bit 4 at the left edge.
 * Digits all reach column 5, giving them the same six-pixel advance.
 */
public final class GenerateVitalsFont {
    private static final String GLYPHS = """
            ! 10101010100010
            " 14141400000000
            # 0A1F0A0A1F0A00
            $ 040F140E051E04
            % 19190204081313
            & 0C12140C15120D
            ' 10100800000000
            ( 04081010100804
            ) 10080404040810
            * 00150E1F0E1500
            + 0004041F040400
            , 00000000001010
            - 0000001F000000
            . 00000000000010
            / 01020204080810
            0 0E11131519110E
            1 040C040404041F
            2 0E11010204081F
            3 1E01010E01011E
            4 02060A121F0202
            5 1F10101E01011E
            6 0E10101E11110E
            7 1F010204080808
            8 0E11110E11110E
            9 0E11110F01010E
            : 00100000001000
            ; 00100000001010
            < 02040810080402
            = 00001F001F0000
            > 10080402040810
            ? 0E110102040004
            @ 0E11171D17100E
            A 0E11111F111111
            B 1E11111E11111E
            C 0F10101010100F
            D 1E11111111111E
            E 1F10101E10101F
            F 1F10101E101010
            G 0F10101711110F
            H 1111111F111111
            I 1C08080808081C
            J 0702020212120C
            K 11121418141211
            L 1010101010101F
            M 111B1515111111
            N 11191915131311
            O 0E11111111110E
            P 1E11111E101010
            Q 0E11111115120D
            R 1E11111E141211
            S 0F10100E01011E
            T 1F040404040404
            U 1111111111110E
            V 11111111110A04
            W 11111115151B11
            X 11110A040A1111
            Y 11110A04040404
            Z 1F01020408101F
            [ 1C10101010101C
            \\ 10080804020201
            ] 1C04040404041C
            ^ 040A1100000000
            _ 0000000000001F
            ` 10080000000000
            a 00000E010F110F
            b 10101E1111111E
            c 00000F1010100F
            d 01010F1111110F
            e 00000E111F100E
            f 0608081C080808
            g 000F11110F010E
            h 10101E11111111
            i 10001010101010
            j 04000404041408
            k 101011121C1211
            l 1810080808081C
            m 00001A15151515
            n 00001E11111111
            o 00000E1111110E
            p 001E11111E1010
            q 000F11110F0101
            r 00001619101010
            s 00000F100E011E
            t 08081C08080806
            u 0000111111110F
            v 00001111110A04
            w 0000111115150A
            x 0000110A040A11
            y 001111110F010E
            z 00001F0204081F
            { 06080810080806
            | 10101010101010
            } 18040402040418
            ~ 0000000D160000
            """;

    public static void main(String[] args) throws Exception {
        BufferedImage atlas = new BufferedImage(128, 48, BufferedImage.TYPE_INT_ARGB);
        boolean[] defined = new boolean[128];
        for (String line : GLYPHS.strip().split("\\R")) {
            char character = line.charAt(0);
            String rows = line.substring(2);
            if (rows.length() != 14 || defined[character]) {
                throw new IllegalArgumentException("Invalid glyph: " + character);
            }
            defined[character] = true;
            int cell = character - 32;
            for (int y = 0; y < 7; y++) {
                int bits = Integer.parseInt(rows.substring(y * 2, y * 2 + 2), 16);
                for (int x = 0; x < 5; x++) {
                    if ((bits & (16 >> x)) != 0) {
                        atlas.setRGB(cell % 16 * 8 + x, cell / 16 * 8 + y, 0xFFFFFFFF);
                    }
                }
            }
        }
        for (int character = 33; character <= 126; character++) {
            if (!defined[character]) throw new IllegalStateException("Missing glyph: " + character);
        }
        Path output = Path.of("src/main/resources/assets/rapidsutils/textures/font/vitals.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(atlas, "png", output.toFile());
    }
}
