package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.HtmlPdfGenerator;
import ru.jamsys.PrepareDataTemplate;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.util.Map;


@Component
@RequestMapping("/html")
public class Html implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Html(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);

//                    UtilFile.writeBytes("web/pdf-03.png", HtmlPdfGenerator.convert(HtmlPdfGenerator.pdf("pdf.html"), 0), FileWriteOptions.CREATE_OR_REPLACE);
//                    HtmlPdfGenerator.merge2image();
//                    input.getResponse().getOutputStream().write(UtilFile.readBytes("web/combined.png"));

                    Map<String, Object> parse = PrepareDataTemplate.parse(
                            UtilFileResource.getAsString("data.json")
                    );

                    @SuppressWarnings("unchecked")
                    Map<String, String> naznParsed = (Map<String, String>) parse.get("NaznParsed");

                    StringBuilder sb = new StringBuilder();
                    naznParsed.forEach((key, value) -> {
                        sb.append("<p class=\"nazn-label\">" + Util.htmlEntity(key) + "</p>\n" +
                                "<p class=\"value\">" + Util.htmlEntity(value) + "</p>");
                    });

                    parse.put("naznRender", sb.toString());

                    input.getResponse().getOutputStream().write(HtmlPdfGenerator.convert(
                            HtmlPdfGenerator.pdf("pdf.html", parse),
                            0
                    ));
                })
                .onComplete((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    input.getCompletableFuture().complete(null);
                })
                .onError((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    App.error(promise.getException());
                    input.complete();
                });
    }


}
