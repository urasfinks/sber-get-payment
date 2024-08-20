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
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@RequestMapping("/")
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
                    Map<String, String> map = input.getHttpRequestReader().getMap();
                    if (map.containsKey("file")) {
                        promise.goTo("readQr");
                        return;
                    }
                    if (map.containsKey("suip") && map.containsKey("date")) {
                        promise.setMapRepository("suip", map.get("suip"));
                        promise.setMapRepository("date", map.get("date"));
                        promise.goTo("payment");
                    }
                    promise.goTo("finishHtml");
                })
                .then("readQr", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    InputStream file = input.getHttpRequestReader().getMultiPartFormData("file");
                    String s = QRReader.readQRCode(file);
                    if (!s.startsWith("{")) {
                        promise.setMapRepository("error", "Не json");
                        promise.goTo("finishHtml");
                        return;
                    }
                    JsonEnvelope<Map<Object, Object>> map = UtilJson.toMap(s);
                    if (map.getException() != null) {
                        promise.setMapRepository("error", "json плохо спарсился");
                        promise.goTo("finishHtml");
                        return;
                    }
                    Map<Object, Object> object = map.getObject();
                    if (!object.containsKey("suip")) {
                        promise.setMapRepository("error", "В QR нет СУИП");
                        promise.goTo("finishHtml");
                        return;
                    }
                    if (!object.containsKey("date")) {
                        promise.setMapRepository("error", "В QR нет даты");
                        promise.goTo("finishHtml");
                        return;
                    }
                    promise.setMapRepository("suip", object.get("suip"));
                    promise.setMapRepository("date", object.get("date"));
                    promise.goTo("payment");
                })
                .then("payment", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    HttpServletResponse response = input.getResponse();
                    try {
                        String location = App.get(ServiceProperty.class).get("run.args.web.resource.location");

                        JasperDesign jasperDesign = JRXmlLoader.load(new File(location + "payment.jrxml"));
                        JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

                        List<DataContainer> dataList = new ArrayList<>();
                        DataContainer sampleBean = new DataContainer();
                        sampleBean.setDetailsMap(PrepareDataTemplate.parse(new String(UtilFile.readBytes("security/data.json"))));
                        dataList.add(sampleBean);
                        JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(dataList);

                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), beanColDataSource);

                        BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, 0, 5f);
                        ImageIO.write(image, "jpg", response.getOutputStream());

                        response.setContentType("image/jpeg");

                    } catch (Throwable th) {
                        App.error(th);
                    }
                    promise.setMapRepository("paymentPrint", true);
                })
                .then("finishHtml", (_, promise) -> {
                    promise.setMapRepository("paymentPrint", false);
                })
                .onComplete((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    if (promise.getRepositoryMap("paymentPrint", Boolean.class)) {
                        input.getCompletableFuture().complete(null);
                    } else {
                        input.setResponseContentType("text/html");
                        input.setBody(TemplateTwix.template(
                                Util.getWebContent("upload.html"),
                                new HashMapBuilder<String, String>()
                                        .append("error", promise.getRepositoryMap("error", String.class, ""))
                        ));
                        input.complete();
                    }
                });
    }

}
