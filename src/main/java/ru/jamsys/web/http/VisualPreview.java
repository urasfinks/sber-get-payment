package ru.jamsys.web.http;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.util.Map;

@Component
@RequestMapping("/")
public class VisualPreview implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public VisualPreview(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    Map<String, String> mapEscaped = input.getHttpRequestReader().getMapEscapedHtmlSpecialChars();
                    if (mapEscaped.containsKey("suip")) {
                        String suip = mapEscaped.get("suip");
                        if (suip == null || suip.isEmpty()) {
                            promise.setMapRepository("error", "СУИП пустой");
                            return;
                        }
                        promise.setMapRepository("suip", suip);
                    }

                    if (mapEscaped.containsKey("date-iso")) { // С формы приходят данные в iso формате
                        mapEscaped.put("date", mapEscaped.get("date-iso"));
                    } else if (mapEscaped.containsKey("date")) { // В QR код зашивается pretty дата
                        promise.setMapRepository("date", Util.timestampToDateFormat(
                                Util.getTimestamp(mapEscaped.get("date"), "dd.MM.yyyy"),
                                "yyyy-MM-dd"
                        ));
                    }
                }).extension(VisualPreview::addHandler);
    }

    public static void addHandler(Promise promise1) {
        promise1.onComplete((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    if (promise.getRepositoryMap("redirect", Boolean.class, false)) {
                        input.getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                        input.getResponse().setHeader("Location", promise.getRepositoryMap("uri", String.class));
                        input.getCompletableFuture().complete(null);
                    } else if (promise.getRepositoryMap("paymentPrint", Boolean.class, false)) {
                        input.getCompletableFuture().complete(null);
                    } else {
                        html(promise);
                    }
                })
                .onError((_, promise) -> {
                    App.error(promise.getException());
                    promise.setMapRepository("error", promise.getException().getMessage());
                    html(promise);
                });
    }

    public static void html(Promise promise) {
        HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
        input.setResponseContentType("text/html");
        String suip = promise.getRepositoryMap("error", String.class, "").isEmpty()
                ? promise.getRepositoryMap("suip", String.class, "")
                : "";
        input.setBody(TemplateTwix.template(
                Util.getWebContent("upload.html"),
                new HashMapBuilder<String, String>()
                        .append("rquid", java.util.UUID.randomUUID().toString())
                        .append("suip", suip)
                        .append("date", promise.getRepositoryMap("date", String.class, Util.getDate("yyyy-MM-dd")))
                        .append("errorShow", promise.getRepositoryMap("error", String.class, "").isEmpty() ? "none" : "table-row")
                        .append("error", promise.getRepositoryMap("error", String.class, ""))
        ));
        input.complete();
    }

}
