package ru.jamsys.web.http;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.DataContainer;
import ru.jamsys.PrepareDataTemplate;
import ru.jamsys.QRReader;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.*;


@Component
@RequestMapping("/**")
public class Main implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Main(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    Map<String, String> mapEscaped = input.getHttpRequestReader().getMapEscapedHtmlSpecialChars();
                    if (mapEscaped.containsKey("date-iso")) {
                        mapEscaped.put("date", Util.timestampToDateFormat(
                                Util.getTimestamp(mapEscaped.get("date-iso"), "yyyy-MM-dd"),
                                "dd.MM.yyyy"
                        ));
                    }
                    if (mapEscaped.containsKey("file")) {
                        promise.goTo("readQr");
                        return;
                    }
                    if (mapEscaped.containsKey("suip")) {
                        String suip = mapEscaped.get("suip");
                        if (suip == null || suip.isEmpty()) {
                            promise.setMapRepository("error", "СУИП пустой");
                            promise.goTo("end");
                            return;
                        }
                        promise.setMapRepository("suip", suip);
                    }
                    if (mapEscaped.containsKey("date")) {
                        promise.setMapRepository("date", Util.timestampToDateFormat(
                                Util.getTimestamp(mapEscaped.get("date"), "dd.MM.yyyy"),
                                "yyyy-MM-dd"
                        ));
                    }
                    if (mapEscaped.containsKey("suip") && mapEscaped.containsKey("date") && mapEscaped.containsKey("find")) {
                        promise.goTo("payment");
                        return;
                    }
                    promise.goTo("end");
                })
                .then("readQr", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    InputStream file = input.getHttpRequestReader().getMultiPartFormData("file");
                    String s = QRReader.readQRCode(file);
                    //System.out.println(input.getHttpRequestReader().getMultiPartFormSubmittedFileName());
                    //File targetFile = new File("web/1.jpg");
                    //OutputStream outStream = new FileOutputStream(targetFile);
                    //outStream.write(file.readAllBytes());
                    //String s = null;

//                    String nameFile = input.getHttpRequestReader().getMultiPartFormSubmittedFileName().get("file");
//                    InputStream file = nameFile.toUpperCase().endsWith(".HEIC")
//                            ?
//
//                    : input.getHttpRequestReader().getMultiPartFormData("file");

                    //String s = QRReader.readQRCode(file);
                    //String s = null;
                    if (s == null) {
                        promise.setMapRepository("error", "QR не распознан");
                        promise.goTo("end");
                    } else if (s.startsWith("{")) {
                        promise.setMapRepository("jsonQr", s);
                        promise.goTo("parseJsonQr");
                    } else if (s.startsWith("http://") || s.startsWith("https://")) {
                        promise.setMapRepository("uri", "/" + s.substring(s.indexOf("?")));
                        promise.setMapRepository("redirect", true);
                        promise.goTo("end");
                    } else {
                        promise.setMapRepository("error", "Не корректный QR");
                        promise.goTo("end");
                    }
                })
                .then("parseJsonQr", (_, promise) -> {
                    String s = promise.getRepositoryMap("jsonQr", String.class);
                    JsonEnvelope<Map<Object, Object>> map = UtilJson.toMap(s);
                    if (map.getException() != null) {
                        promise.setMapRepository("error", "При обработке QR возникли ошибки");
                        promise.goTo("end");
                        return;
                    }
                    Map<Object, Object> object = map.getObject();
                    if (!object.containsKey("suip")) {
                        promise.setMapRepository("error", "QR не содержит СУИП");
                        promise.goTo("end");
                        return;
                    }
                    if (!object.containsKey("date")) {
                        promise.setMapRepository("error", "QR не содержит даты");
                        promise.goTo("end");
                        return;
                    }
                    promise.setMapRepository("suip", object.get("suip"));
                    promise.setMapRepository("date", object.get("date"));
                    promise.goTo("payment");
                })
                .then("payment", (_, promise) -> {

                    String suip = promise.getRepositoryMap("suip", String.class);
                    if (suip == null || !suip.equals("100776404158ZNSW")) {
                        promise.setMapRepository("error", "СУИП не найден");
                        promise.goTo("end");
                        return;
                    }

                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    HttpServletResponse response = input.getResponse();

                    JasperDesign jasperDesign = JRXmlLoader.load(UtilFileResource.get("payment.jrxml"));
                    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

                    List<DataContainer> dataList = new ArrayList<>();
                    DataContainer sampleBean = new DataContainer();
                    //sampleBean.setDetailsMap(PrepareDataTemplate.parse(new String(UtilFile.readBytes("security/data.json"))));
                    sampleBean.setDetailsMap(PrepareDataTemplate.parse(
                            UtilFileResource.getAsString("data.json")
                    ));
                    dataList.add(sampleBean);
                    JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(dataList);

                    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), beanColDataSource);

                    BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, 0, 5f);
                    ImageIO.write(image, "jpg", response.getOutputStream());

                    response.setContentType("image/jpeg");

                    promise.setMapRepository("paymentPrint", true);
                    promise.goTo("end");
                })
                .then("end", (_, _) -> {
                    //Терминальный
                })
                .onComplete((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    if (promise.getRepositoryMap("redirect", Boolean.class, false)) {
                        input.getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                        input.getResponse().setHeader("Location", promise.getRepositoryMap("uri", String.class));
                        input.getCompletableFuture().complete(null);
                    } else if (promise.getRepositoryMap("paymentPrint", Boolean.class, false)) {
                        input.getCompletableFuture().complete(null);
                    } else {
                        input.setResponseContentType("text/html");
                        String suip = promise.getRepositoryMap("error", String.class, "").isEmpty()
                                ? promise.getRepositoryMap("suip", String.class, "")
                                : "";
                        input.setBody(TemplateTwix.template(
                                Util.getWebContent("upload.html"),
                                new HashMapBuilder<String, String>()
                                        .append("rquid", UUID.randomUUID().toString())
                                        .append("suip", suip)
                                        .append("date", promise.getRepositoryMap("date", String.class, Util.getDate("yyyy-MM-dd")))
                                        .append("errorShow", promise.getRepositoryMap("error", String.class, "").isEmpty() ? "none" : "table-row")
                                        .append("error", promise.getRepositoryMap("error", String.class, ""))
                        ));
                        input.complete();
                    }
                })
                .onError((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    App.error(promise.getException());
                    promise.setMapRepository("error", promise.getException().getMessage());
                    input.setResponseContentType("text/html");
                    input.setBody(TemplateTwix.template(
                            Util.getWebContent("upload.html"),
                            new HashMapBuilder<String, String>()
                                    .append("rquid", java.util.UUID.randomUUID().toString())
                                    .append("suip", "")
                                    .append("date", promise.getRepositoryMap("date", String.class, Util.getDate("yyyy-MM-dd")))
                                    .append("errorShow", promise.getRepositoryMap("error", String.class, "").isEmpty() ? "none" : "table-row")
                                    .append("error", promise.getRepositoryMap("error", String.class, ""))
                    ));
                    input.complete();
                });
    }


}
