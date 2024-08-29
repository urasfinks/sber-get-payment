package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequestMapping
public class CheckPage implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CheckPage(ServicePromise servicePromise) {
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
                    String imgReq = UtilJson.toString(object, "{}");
                    if (imgReq == null) {
                        promise.setRepositoryMap("error", "При генерации ссылки на изображении произошла ошибка");
                        return;
                    }
                    promise.setRepositoryMap("json", "/CheckPdf?json=" + URLEncoder.encode(imgReq, StandardCharsets.UTF_8));
                })
                .extension(promise -> VisualPreview.addHandler(promise, "check.html", "upload.html"));
    }


}
