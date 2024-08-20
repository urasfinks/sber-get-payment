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
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.UtilFile;
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
@SuppressWarnings("unused")
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
                .append("input", (atomicBoolean, promise) -> {
                    //input.setBody(Util.getWebContent("upload.html"));
                    //input.setResponseContentType("text/html");
                })
                .onComplete((atomicBoolean, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    HttpServletResponse response = input.getResponse();
//                    HttpServletResponse response = input.getResponse();
//                    response.setHeader("Content-Type", "application/pdf");
//                    response.setHeader("Content-Disposition", "inline; filename=\"payment.pdf\"");
//
                    String location = App.get(ServiceProperty.class).get("run.args.web.resource.location");
                    try {
                        Map<String, Object> parameters = new HashMap<>();

                        JasperDesign jasperDesign = JRXmlLoader.load(new File(location + "payment.jrxml"));
                        JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

                        List<DataContainer> dataList = new ArrayList<>();
                        DataContainer sampleBean = new DataContainer();
                        sampleBean.setDetailsMap(PrepareDataTemplate.parse(new String(UtilFile.readBytes("security/data.json"))));
                        dataList.add(sampleBean);
                        JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(dataList);


                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanColDataSource);

                        //JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());

                        BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, 0, 5f);
                        ImageIO.write(image, "jpg", response.getOutputStream());

                        response.setContentType("image/jpeg");
                        //response.addHeader("Content-Disposition", "inline; filename=jasper.pdf;");
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }

                    input.getCompletableFuture().complete(null);
                });
    }

}
