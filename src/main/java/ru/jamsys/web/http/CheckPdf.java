package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.HtmlPdfGenerator;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.util.HashMap;
import java.util.Map;

@Component
@RequestMapping
public class CheckPdf implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CheckPdf(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
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
                    naznParsed.forEach((key, value) -> sb
                            .append("<p class=\"nazn-label\">")
                            .append(Util.htmlEntity(key)).append("</p>\n")
                            .append("<p class=\"value\">")
                            .append(Util.htmlEntity(value))
                            .append("</p>")
                    );
                    parse.put("naznRender", sb.toString());
                    servletHandler.getResponse().getOutputStream().write(HtmlPdfGenerator.convert(
                            HtmlPdfGenerator.pdf("pdf.html", parse),
                            0
                    ));
                    promise.setRepositoryMap("paymentPrint", true);
                })
                .extension(promise -> VisualPreview.addHandler(promise, "upload.html", "upload.html"));
    }


}
