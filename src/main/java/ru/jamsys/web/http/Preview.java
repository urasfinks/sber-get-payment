package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PrepareDataTemplate;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerVirtualFileSystem;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileLoaderFactory;
import ru.jamsys.core.resource.virtual.file.system.view.FileViewKeyStore;
import ru.jamsys.core.web.http.HttpHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
@RequestMapping
public class Preview implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    private ManagerVirtualFileSystem managerVirtualFileSystem;

    private final String jksVirtualPath = "/hybrid.jks";

    private final String jksSecurityAlias = "hybrid.cert.password";

    private final String url;

    public Preview(
            ApplicationContext applicationContext,
            ServicePromise servicePromise,
            ManagerVirtualFileSystem managerVirtualFileSystem
    ) {
        this.servicePromise = servicePromise;
        this.managerVirtualFileSystem = managerVirtualFileSystem;
        String jksPath = applicationContext.getBean(ServiceProperty.class).get("hybrid.security.path");
        this.url = applicationContext.getBean(ServiceProperty.class).get(
                String.class,
                "hybrid.url",
                "http://localhost/Mirror"
        );
        managerVirtualFileSystem.add(
                new File(jksVirtualPath, FileLoaderFactory.fromFileSystem(jksPath))
        );
    }

    @Override
    public Promise generate() { // {"suip":"100776404158ZNSW","date":"15.08.2024"}
        return servicePromise.get(index, 25_000L)
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
                .appendWithResource("http", HttpResource.class, (_, promise, httpResource) -> {
                    if(!promise.getRepositoryMap("error", String.class, "").isEmpty()){
                        return;
                    }
                    HttpClientImpl client = new HttpClientImpl();
                    client.setUrl(url);
                    client.setKeyStore(
                            managerVirtualFileSystem.get(jksVirtualPath),
                            FileViewKeyStore.prop.SECURITY_KEY.name(), jksSecurityAlias,
                            FileViewKeyStore.prop.TYPE.name(), "JKS"
                    );
                    client.setRequestHeader("Content-type", "application/json");
                    client.setRequestHeader("Timeout", "10000");
                    client.setRequestHeader("servicename", "common");

                    String rquid = UUID.randomUUID().toString().replace("-", "");
                    client.setRequestHeader("Rquid", rquid);

                    String postData = "{\n" +
                            "  \"RqTm\": \"2023-08-24T10:22:16.216+03:00\",\n" +
                            "  \"SPName\": \"PAYMENT_PRO\",\n" +
                            "  \"SearchParams\": {\n" +
                            "    \"DateBegin\": \"" + promise.getRepositoryMap("date", String.class) + "T00:00:00\",\n" +
                            "    \"DateEnd\": \"" + promise.getRepositoryMap("date", String.class) + "T23:59:59\",\n" +
                            "    \"SUIPPayInfoList\": [\n" +
                            "      {\n" +
                            "        \"SUIPPayInfo\": {\n" +
                            "          \"SUIP\": \"" + promise.getRepositoryMap("suip", String.class) + "\"\n" +
                            "        }\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  }\n" +
                            "}";
                    client.setPostData(postData.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Rquid: " + rquid + "; Request: " + postData);
                    HttpResponse execute = httpResource.execute(client);
                    String body = execute.getBody();
                    if (body == null || body.isEmpty()) {
                        throw new RuntimeException("Нет ответа от сервиса");
                    }
                    System.out.println(UtilBase64.base64Encode(body, true));
                    if (!body.contains("SUIP") || !body.contains("PayeeInfo")) {
                        throw new RuntimeException("Не найдено");
                    }
                    Map<String, Object> parse = PrepareDataTemplate.parse(body);
                    String ret = UtilJson.toStringPretty(parse, "{}");
                    promise.setRepositoryMap("json", ret);
                })

//                .then("getJson", (_, promise) -> {
//                    if(!promise.getRepositoryMap("error", String.class, "").isEmpty()){
//                        return;
//                    }
//                    Map<String, Object> parse = PrepareDataTemplate.parse(
//                            UtilFileResource.getAsString("data2.json")
//                    );
//                    promise.setRepositoryMap("json", UtilJson.toStringPretty(parse, "{}"));
//                })

                .extension(promise -> VisualPreview.addHandler(promise, "preview.html", "upload.html"));
    }

}
