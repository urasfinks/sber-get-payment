package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PrepareDataTemplate;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
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
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> mapEscaped = servletHandler.getRequestReader().getMapEscapedHtmlSpecialChars();

                    if (!mapEscaped.containsKey("suip")) {
                        promise.setRepositoryMap("error", "Нет СУИП");
                        return;
                    }
                    String suip = mapEscaped.get("suip");
                    if (suip == null || suip.isEmpty()) {
                        promise.setRepositoryMap("error", "СУИП пустой");
                        return;
                    }
                    if (!suip.equals("100776404158ZNSW")) {
                        promise.setRepositoryMap("error", "СУИП не найден");
                        return;
                    }
                    promise.setRepositoryMap("suip", suip);


                    if (!mapEscaped.containsKey("date-iso")) { // С формы приходят данные в iso формате
                        promise.setRepositoryMap("error", "Нет даты");
                        return;
                    }
                    String date = mapEscaped.get("date-iso");
                    if (date == null || date.isEmpty()) {
                        promise.setRepositoryMap("error", "Дата пустая");
                        return;
                    }
                    promise.setRepositoryMap("date", mapEscaped.get("date-iso"));
                })
                .then("getJson", (_, promise) -> {
                    if(!promise.getRepositoryMap("error", String.class, "").isEmpty()){
                        return;
                    }
                    Map<String, Object> parse = PrepareDataTemplate.parse(
                            UtilFileResource.getAsString("data.json")
                    );
                    promise.setRepositoryMap("json", UtilJson.toStringPretty(parse, "{}"));
                })
                .extension(promise -> VisualPreview.addHandler(promise, "preview.html", "upload.html"));
    }

}
