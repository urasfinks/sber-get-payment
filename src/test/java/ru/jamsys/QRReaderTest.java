package ru.jamsys;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilBase64Url;
import ru.jamsys.core.flat.util.crypto.UtilAes;
import ru.jamsys.core.flat.util.crypto.UtilRsa;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

class QRReaderTest {

    @Test
    public void test() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, WriterException {
        String suip = "100776404158ZNSW";
        String date = "202410101528";

        IvParameterSpec ivParameterSpec = UtilAes.generateIv();
        SecretKey secretKey = UtilAes.generateKey(128);
        byte[] aes = UtilAes.encrypt(UtilAes.algorithm, (date + suip).getBytes(), secretKey, ivParameterSpec);

        //        UtilRsa.alg = "EC";
//        UtilRsa.signAlg = "SHA256withECDSA";
        UtilRsa.size = 512;
        KeyPair keyPair = UtilRsa.genKeyPair();
        byte[] sign = UtilRsa.sign(new String(aes), keyPair.getPrivate());

//        String d = new BigInteger(1, aes).toString(16);
//        String s = new BigInteger(1, sign).toString(16);

        String d = UtilBase64Url.encode(aes);
        String s = UtilBase64Url.encode(sign);

        System.out.println("aes: " + d.length() + "; sign:" + s.length());
        String url = "https://qr.sber.ru/?d=" + d + "&s=" + s;
        System.out.println(url);
        //https://suip-check.ru/?d=1S5+dV1Bx2z5U8oW6/sRL9Gp1Y1FeUkmmnLEn8hHS0M=&s=bCmDGuT22Yxc3umsY49K9/gb60xsI65sUpv1fL1qfOIehE5UjnC1X84ZFoLanuQQ2mdMvk4Mdw8IXzbBKsIhFQ==
        //https://suip-check.ru/?d=20ce1a8b81e455b4c8596bf836bc3a65bb89c0d4132c9a6373a5bf3e69103cee&s=8bc12fb2311898b0afbb3579979cba4145150479ee618ab5208b00cdd681690c45551b1368de889566d4f56af1be5bce2f7921fd5cd9f7c37131cee560d96759
        generateQRBitMap(url);
    }

    public static void generateQRBitMap(String data) throws IOException, WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.QR_COMPACT, true);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 150, 150, hints);
        Path path = FileSystems.getDefault().getPath("q1.png");
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

}