package ru.jamsys;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilHide;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrepareDataTemplate {

    public static void main(String[] args) throws IOException {
        byte[] bytes = UtilFile.readBytes("security/data.json");
        System.out.println(UtilJson.toStringPretty(parse(new String(bytes)), "{}"));
    }

    public static Map<String, Object> parse(String json) {
        Map<Integer, String> mapOper = new HashMapBuilder<Integer, String>()
                .append(1, "Оплата услуг наличными")
                .append(2, "Безналичная оплата услуг")
                .append(3, "Безналичная оплата услуг");

        Map<Integer, String> mapType = new HashMapBuilder<Integer, String>()
                .append(1, "Наличные")
                .append(2, "Карта")
                .append(3, "Счёт");

        Map<String, Object> result = new LinkedHashMap<>();
        UtilJson.selector(
                json,
                new HashMapBuilder<String, String>()
                        .append("C_PAY_CONFIRM_NARRATIVE", "$.Result.PaymentInfoList[0].Narrative")
                        .append("C_PAY_CONFIRM_PAYDATE", "$.Result.PaymentInfoList[0].PaymentDate")
                        .append("PRIVATEPAY_CONV_PRIVATEPAYID", "$.Result.PaymentInfoList[0].SUIP")
                        .append("PAYORDER_CONV_RECBANKACC", "$.Result.PaymentInfoList[0].PayeeInfo.BankInfo.BankAcc")
                        .append("PAYORDER_CONV_RECBANKBIC", "$.Result.PaymentInfoList[0].PayeeInfo.BankInfo.BIC")
                        .append("PAYORDER_CONV_RECBANKNAME", "$.Result.PaymentInfoList[0].PayeeInfo.BankInfo.BankName")
                        .append("PAYORDER_CONV_RECACC", "$.Result.PaymentInfoList[0].PayeeInfo.Account")
                        .append("PAYORDER_CONV_RECNAME", "$.Result.PaymentInfoList[0].PayeeInfo.ServiceName")
                        .append("PAYORDER_CONV_RECINN", "$.Result.PaymentInfoList[0].PayeeInfo.INN")
                        .append("PAYORDER_CONV_NDOC", "$.Result.PaymentInfoList[0].DocNumber")
                        .append("C_PAY_CONFIRM_OPERDATE", "$.Result.PaymentInfoList[0].DocDate")
                        .append("PayMethodCode", "$.Result.PaymentInfoList[0].PayMethodCode") // Способ оплаты
                        .append("C_PAY_CONFIRM_CHANNEL", "$.Result.PaymentInfoList[0].ChannelType") // Канал приёма платежа
                        .append("C_PAY_CONFIRM_SUM", "$.Result.PaymentInfoList[0].Sum")
                        .append("PRIVATEPAY_CONV_FIO", "$.Result.PaymentInfoList[0].PayerInfo.FIO")
                , result);

        CurrencyDto CURRENCY_RUB = CurrencyDto.builder()
                .name1("РУБЛЬ")
                .name2to4("РУБЛЯ")
                .name5to20("РУБЛЕЙ")
                .fractionalName1("КОПЕЙКА")
                .fractionalName2to4("КОПЕЙКИ")
                .fractionalName5to20("КОПЕЕК")
                .build();

        try {
            result.put("C_PAY_CONFIRM_OPERDATE", Util.timestampToDateFormat(
                    Util.getTimestamp(result.get("C_PAY_CONFIRM_OPERDATE") + "", "yyyy-MM-dd"),
                    "dd.MM.yyyy"
            ));
        } catch (Throwable th) {
            App.error(th);
        }

        try {
            result.put("C_PAY_CONFIRM_PAYDATE", Util.timestampToDateFormat(
                    Util.getTimestamp(result.get("C_PAY_CONFIRM_PAYDATE") + "", "yyyy-MM-dd'T'HH:mm:ssXXX"),
                    "dd.MM.yyyy HH:mm"
            ));
        } catch (Throwable th) {
            App.error(th);
        }

        result.put("amountString", FwMoneyUtils.num2str(new BigDecimal(result.get("C_PAY_CONFIRM_SUM") + ""), CURRENCY_RUB));
        result.put("C_PAY_CONFIRM_SUM", String.format("%.2f", Float.parseFloat(result.get("C_PAY_CONFIRM_SUM") + "")));
        result.put("PAYORDER_CONV_RECKPP", "0");
        result.put("C_PAY_CONFIRM_DATE", Util.getDate("dd.MM.yyyy HH:mm"));
        result.put("C_PAY_CONFIRM_PAYMETH_TYPE", mapOper.get(result.get("PayMethodCode")));
        result.put("A_PAYMETH_TXT", mapType.get(result.get("PayMethodCode")));

        // Маскировка
        //String star = Character.toString((char)0x2217);
        String star = "*";
        result.put("PRIVATEPAY_CONV_FIO", UtilHide.explodeLetterAndMask((String) result.get("PRIVATEPAY_CONV_FIO"), 2,4,30, star).replace(" ", "  "));
        result.put("C_PAY_CONFIRM_NARRATIVE", UtilHide.explodeLetterAndMask((String) result.get("C_PAY_CONFIRM_NARRATIVE"), 1,4,40, star));

        return result;
    }

}
