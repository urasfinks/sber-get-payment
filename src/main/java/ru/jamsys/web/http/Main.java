package ru.jamsys.web.http;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.io.FileInputStream;
import java.io.InputStream;

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
                    response.setHeader("Content-Type", "application/pdf");
                    response.setHeader("Content-Disposition", "inline; filename=\"payment.pdf\"");

                    String location = App.get(ServiceProperty.class).get("run.args.web.resource.location");
                    InputStream in = new FileInputStream(location + "test.pdf");
                    input.complete(in);
                });
    }

}
