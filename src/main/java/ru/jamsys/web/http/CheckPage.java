package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
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
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    Map<String, String> maps = input.getHttpRequestReader().getMap();
                    if (!maps.containsKey("json")) {
                        promise.setMapRepository("error", "Пустая форма");
                        return;
                    }
                    JsonEnvelope<Map<Object, Object>> jsonEnvelope = UtilJson.toMap(maps.get("json"));
                    if (jsonEnvelope.getException() != null) {
                        promise.setMapRepository("error", "При обработке QR возникли ошибки");
                        return;
                    }
                    Map<Object, Object> object = jsonEnvelope.getObject();
                    promise.setMapRepository("json", "/CheckPdf?json=" + URLEncoder.encode(UtilJson.toString(object, "{}"), StandardCharsets.UTF_8));
                })
                .extension(promise -> VisualPreview.addHandler(promise, "check.html", "upload.html"));
    }


}
