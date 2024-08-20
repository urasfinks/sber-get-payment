package ru.jamsys;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import ru.jamsys.core.App;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class QRReader {

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println(QRReader.readQRCode(new FileInputStream("web/qr.png")));
    }

    public static String readQRCode(InputStream is) {
        String encodedContent = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(is);
            BufferedImageLuminanceSource bufferedImageLuminanceSource = new BufferedImageLuminanceSource(bufferedImage);
            HybridBinarizer hybridBinarizer = new HybridBinarizer(bufferedImageLuminanceSource);
            BinaryBitmap binaryBitmap = new BinaryBitmap(hybridBinarizer);
            MultiFormatReader multiFormatReader = new MultiFormatReader();

            Result result = multiFormatReader.decode(binaryBitmap);
            encodedContent = result.getText();
        } catch (Throwable th) {
            App.error(th);
        }
        return encodedContent;
    }

}
