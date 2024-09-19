package ru.jamsys.web.http;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilFile;
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
                .then("init", (_, _, promise) -> {
                    ServletHandler input = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> mapEscaped = input.getRequestReader().getMapEscapedHtmlSpecialChars();
                    if (mapEscaped.containsKey("suip")) {
                        String suip = mapEscaped.get("suip");
                        if (suip == null || suip.isEmpty()) {
                            promise.setRepositoryMap("error", "СУИП пустой");
                            return;
                        }
                        promise.setRepositoryMap("suip", suip);
                    }

                    if (mapEscaped.containsKey("date")) { // В QR код зашивается pretty дата
                        promise.setRepositoryMap("date", UtilDate.timestampFormat(
                                UtilDate.getTimestamp(mapEscaped.get("date"), "dd.MM.yyyy"),
                                "yyyy-MM-dd"
                        ));
                    }
                }).extension(VisualPreview::addHandler);
    }

    public static void addHandler(Promise promiseSource) {
        addHandler(promiseSource, "upload.html", "upload.html");
    }

    public static void addHandler(Promise promiseSource, String pathHtmlSuccess, String pathHtmlError) {
        promiseSource.onComplete((_, _, promise) -> {
                    ServletHandler input = promise.getRepositoryMapClass(ServletHandler.class);
                    if (promise.getRepositoryMap(Boolean.class, "redirect", false)) {
                        input.setResponseStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                        input.setResponseHeader("Location", promise.getRepositoryMap(String.class, "uri"));
                        input.getCompletableFuture().complete(null);
                    } else if (promise.getRepositoryMap(Boolean.class, "paymentPrint", false)) {
                        input.getCompletableFuture().complete(null);
                    } else {
                        html(promise, pathHtmlSuccess);
                    }
                })
                .onError((_, _, promise) -> {
                    String message = promise.getException().getMessage();
                    if (message == null || message.isEmpty()) {
                        promise.setRepositoryMap("error", "Не предвиденная ошибка");
                    } else {
                        promise.setRepositoryMap("error", promise.getException().getMessage());
                    }
                    App.error(promise.getException());

                    html(promise, pathHtmlError);
                });
    }

    public static void html(Promise promise, String pathHtml) {
        ServletHandler input = promise.getRepositoryMapClass(ServletHandler.class);
        input.setResponseContentType("text/html");
        String suip = promise.getRepositoryMap(String.class, "error", "").isEmpty()
                ? promise.getRepositoryMap(String.class, "suip", "")
                : "";
        input.setResponseBody(TemplateTwix.template(
                UtilFile.getWebContent(pathHtml),
                new HashMapBuilder<String, String>()
                        .append("rquid", java.util.UUID.randomUUID().toString())
                        .append("suip", suip)
                        .append("date", promise.getRepositoryMap(String.class, "date", UtilDate.get("yyyy-MM-dd")))
                        .append("errorShow", promise.getRepositoryMap(String.class, "error", "").isEmpty() ? "none" : "table-row")
                        .append("error", promise.getRepositoryMap(String.class, "error", ""))
                        .append("json", promise.getRepositoryMap(String.class, "json", "{}"))
        ));
        input.responseComplete();
    }

}
