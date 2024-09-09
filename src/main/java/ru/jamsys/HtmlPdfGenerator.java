package ru.jamsys;

import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.render.Box;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HtmlPdfGenerator {

    public static void main(String[] args) throws Exception {
//        UtilFile.writeBytes(
//                "web/pdf-original.png",
//                convert(UtilFile.readBytes("web/check.pdf"), 0),
//                FileWriteOptions.CREATE_OR_REPLACE
//        );
        //UtilFile.writeBytes("web/pdf-01.png", convert(performPdfDocument("web/pdf.html"), 0), FileWriteOptions.CREATE_OR_REPLACE);
        //UtilFile.writeBytes("web/pdf-02.png", convert(createPdfFromText("web/pdf.html"), 0), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("web/pdf-03.png", convert(pdf("web/pdf.html", new HashMap<>()), 0), FileWriteOptions.CREATE_OR_REPLACE);
        merge2image();


    }

    private static final int PDF_DOTS_PER_PIXEL = 75;

    public static byte[] pdf(String path, Map<String, Object> data) throws Exception {

        System.out.println(UtilJson.toStringPretty(data, "{}"));

        /*
        * {
  "Nazn" : "P****NAME: Ф***ЛИЯ И*Я О****ТВО; P***OSE: Д*Я Т**ТА; E***UDE_R**S: E***UDE_R**S@I****ANCE_P**S_R*@;",
  "PaymentDate" : "15.08.2024 10:41",
  "SUIP" : "100776404158ZNSW",
  "BankAcc" : "30101810600000000608",
  "BIC" : "040813608",
  "BankName" : "ДАЛЬНЕВОСТОЧНЫЙ БАНК ПАО СБЕРБАНК",
  "Account" : "40702810670000000034",
  "ServiceName" : "Лёвина Радость_ЦСР_РКО_един",
  "INN" : "8848116085",
  "DocNumber" : "4",
  "DocDate" : "15.08.2024",
  "PayMethodCode" : 2,
  "ChannelType" : "СберБанк онлайн",
  "Sum" : "100,00",
  "FIO" : "О**ов  А***сей  Т****вич",
  "AmountString" : "СТО РУБЛЕЙ 00 КОПЕЕК",
  "KPP" : "0",
  "Now" : "24.08.2024 21:29",
  "PaymentOperation" : "Безналичная оплата услуг",
  "PaymentType" : "Карта"
}
        * */

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();

        builder.useFont(UtilFile.getWebFile("ArialRegular.ttf"), "Arial");
        builder.useFont(UtilFile.getWebFile(("rouble.ttf")), "ALS Rubl");

        String html = UtilFile.getWebContent(path);

        Map<String, String> sMap = new LinkedHashMap<>();
        data.forEach((key, value) -> sMap.put(key, value.toString()));



        html = TemplateTwix.template(html, sMap);

        builder.withHtmlContent(html, null);

        PdfBoxRenderer renderer = builder.buildPdfRenderer();
        renderer.layout();
        Box box = renderer.getRootBox().getChild(0).getChild(0);

        html = html.replace(
                "size:112.9mm 1393.3mm;",
                "size:" + (Math.round(box.getWidth() / PDF_DOTS_PER_PIXEL)) + "mm " + (Math.round(box.getHeight() / PDF_DOTS_PER_PIXEL)) + "mm;"
        );

        //---------------
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        builder.toStream(outputStream);
        PdfRendererBuilder builder2 = new PdfRendererBuilder();

        builder2.useFont(UtilFile.getWebFile("ArialRegular.ttf"), "Arial");
        builder2.useFont(UtilFile.getWebFile(("rouble.ttf")), "ALS Rubl");

        builder.withHtmlContent(html, "/");
        builder.run();
        System.out.println("PDF created");
        return outputStream.toByteArray();
    }


    public static byte[] convert(byte[] pdf, int page) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf));
        //PDDocument document = Loader.loadPDF(pdf);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 400, ImageType.RGB);
        ImageIO.write(bim, "jpg", outputStream);
        document.close();
        byte[] result = outputStream.toByteArray();
        outputStream.close();
        return result;
    }

    public static void merge2image() throws IOException {
        BufferedImage image = ImageIO.read(new File("web/pdf-03.png"));
        BufferedImage overlay = ImageIO.read(new File("web/pdf-original.png"));
        int w = Math.max(image.getWidth(), overlay.getWidth());
        int h = Math.max(image.getHeight(), overlay.getHeight());
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = combined.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.drawImage(overlay, 0, 0, null);
        g.dispose();
        ImageIO.write(combined, "PNG", new File("web/combined.png"));
    }

}
