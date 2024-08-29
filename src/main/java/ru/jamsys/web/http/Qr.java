package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.QRReader;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.io.InputStream;
import java.util.Map;

@Component
@RequestMapping
public class Qr implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Qr(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    InputStream file = servletHandler.getRequestReader().getMultiPartFormData("file");
                    String s = QRReader.readQRCode(file);
                    if (s == null) {
                        promise.setRepositoryMap("error", "QR не распознан");
                    } else if (s.startsWith("{")) {
                        parseQr(s, promise);
                    } else if (s.startsWith("http://") || s.startsWith("https://")) {
                        promise.setRepositoryMap("uri", "/" + s.substring(s.indexOf("?")));
                        promise.setRepositoryMap("redirect", true);
                    } else {
                        promise.setRepositoryMap("error", "Не корректный QR");
                    }
                }).extension(VisualPreview::addHandler);
    }

    public static void parseQr(String s, Promise promise) {
        JsonEnvelope<Map<Object, Object>> map = UtilJson.toMap(s);
        if (map.getException() != null) {
            promise.setRepositoryMap("error", "При обработке QR возникли ошибки");
            return;
        }
        Map<Object, Object> object = map.getObject();
        if (!object.containsKey("suip")) {
            promise.setRepositoryMap("error", "QR не содержит СУИП");
            return;
        }
        if (!object.containsKey("date")) {
            promise.setRepositoryMap("error", "QR не содержит даты");
            return;
        }

        String suip = Util.htmlEntity((String) object.get("suip"));
        String date = Util.htmlEntity((String) object.get("date"));

        promise.setRepositoryMap("uri", "/?suip=" + suip + "&date=" + date);
        promise.setRepositoryMap("redirect", true);
    }

}
