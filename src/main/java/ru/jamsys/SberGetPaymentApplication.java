package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class SberGetPaymentApplication {

    public static void main(String[] args) {
        App.springSource = SberGetPaymentApplication.class;
        App.main(args);
    }

}
