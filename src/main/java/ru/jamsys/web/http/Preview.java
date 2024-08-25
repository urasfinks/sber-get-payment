package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PrepareDataTemplate;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.util.Map;

@Component
@RequestMapping
public class Preview implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Preview(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    Map<String, String> mapEscaped = input.getHttpRequestReader().getMapEscapedHtmlSpecialChars();

                    if (!mapEscaped.containsKey("suip")) {
                        promise.setMapRepository("error", "Нет СУИП");
                        return;
                    }
                    String suip = mapEscaped.get("suip");
                    if (suip == null || suip.isEmpty()) {
                        promise.setMapRepository("error", "СУИП пустой");
                        return;
                    }
                    if (!suip.equals("100776404158ZNSW")) {
                        promise.setMapRepository("error", "СУИП не найден");
                        return;
                    }
                    promise.setMapRepository("suip", suip);


                    if (!mapEscaped.containsKey("date-iso")) { // С формы приходят данные в iso формате
                        promise.setMapRepository("error", "Нет даты");
                        return;
                    }
                    String date = mapEscaped.get("date-iso");
                    if (date == null || date.isEmpty()) {
                        promise.setMapRepository("error", "Дата пустая");
                        return;
                    }
                    promise.setMapRepository("date", mapEscaped.get("date-iso"));
                })
                .then("getJson", (atomicBoolean, promise) -> {
                    if(!promise.getRepositoryMap("error", String.class, "").isEmpty()){
                        return;
                    }
                    Map<String, Object> parse = PrepareDataTemplate.parse(
                            UtilFileResource.getAsString("data.json")
                    );
                    promise.setMapRepository("json", UtilJson.toStringPretty(parse, "{}"));
                })
                .extension(promise -> VisualPreview.addHandler(promise, "preview.html", "upload.html"));
    }


}
