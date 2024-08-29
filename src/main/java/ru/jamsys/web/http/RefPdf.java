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
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequestMapping
public class RefPdf implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public RefPdf(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMap(ServletHandler.class);
                    Map<String, String> maps = servletHandler.getRequestReader().getMap();
                    if (!maps.containsKey("json")) {
                        promise.setRepositoryMap("error", "Пустая форма");
                        return;
                    }
                    JsonEnvelope<Map<Object, Object>> jsonEnvelope = UtilJson.toMap(maps.get("json"));
                    if (jsonEnvelope.getException() != null) {
                        promise.setRepositoryMap("error", "При обработке QR возникли ошибки");
                        return;
                    }

                    Map<Object, Object> object = jsonEnvelope.getObject();
                    Map<String, Object> parse = new HashMap<>();
                    object.forEach((o, o2) -> parse.put((String) o, o2));

                    @SuppressWarnings("unchecked")
                    Map<String, String> naznParsed = (Map<String, String>) parse.get("Nazn");

                    StringBuilder sb = new StringBuilder();
                    naznParsed.forEach((key, value) -> sb.append(key).append(": ").append(value).append("; "));
                    parse.put("Nazn", sb.toString());


                    String location = App.get(ServiceProperty.class).get("run.args.web.resource.location");
                    JasperDesign jasperDesign = JRXmlLoader.load(new File(location + "payment.jrxml"));
                    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

                    List<DataContainer> dataList = new ArrayList<>();
                    DataContainer sampleBean = new DataContainer();
                    sampleBean.setDetailsMap(parse);
                    dataList.add(sampleBean);
                    JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(dataList);

                    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), beanColDataSource);

                    BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, 0, 5f);
                    HttpServletResponse response = servletHandler.getResponse();
                    ImageIO.write(image, "jpg", response.getOutputStream());


                    promise.setRepositoryMap("paymentPrint", true);
                })
                .extension(promise -> VisualPreview.addHandler(promise, "upload.html", "upload.html"));
    }

}
