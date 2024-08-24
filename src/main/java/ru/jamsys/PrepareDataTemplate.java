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
                        .append("SumFee", "$.Result.PaymentInfoList[0].SumFee")
                        .append("Nazn", "$.Result.PaymentInfoList[0].Narrative")
                        .append("PaymentDate", "$.Result.PaymentInfoList[0].PaymentDate")
                        .append("SUIP", "$.Result.PaymentInfoList[0].SUIP")
                        .append("BankAcc", "$.Result.PaymentInfoList[0].PayeeInfo.BankInfo.BankAcc")
                        .append("BIC", "$.Result.PaymentInfoList[0].PayeeInfo.BankInfo.BIC")
                        .append("BankName", "$.Result.PaymentInfoList[0].PayeeInfo.BankInfo.BankName")
                        .append("Account", "$.Result.PaymentInfoList[0].PayeeInfo.Account")
                        .append("ServiceName", "$.Result.PaymentInfoList[0].PayeeInfo.ServiceName")
                        .append("NameReceiver", "$.Result.PaymentInfoList[0].PayeeInfo.NameReceiver")
                        .append("INN", "$.Result.PaymentInfoList[0].PayeeInfo.INN")
                        .append("DocNumber", "$.Result.PaymentInfoList[0].DocNumber")
                        .append("DocDate", "$.Result.PaymentInfoList[0].DocDate")
                        .append("PayMethodCode", "$.Result.PaymentInfoList[0].PayMethodCode") // Способ оплаты
                        .append("ChannelType", "$.Result.PaymentInfoList[0].ChannelType") // Канал приёма платежа
                        .append("Sum", "$.Result.PaymentInfoList[0].Sum")
                        .append("FIO", "$.Result.PaymentInfoList[0].PayerInfo.FIO")
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
            result.put("DocDate", Util.timestampToDateFormat(
                    Util.getTimestamp(result.get("DocDate") + "", "yyyy-MM-dd"),
                    "dd.MM.yyyy"
            ));
        } catch (Throwable th) {
            App.error(th);
        }

        try {
            result.put("PaymentDate", Util.timestampToDateFormat(
                    Util.getTimestamp(result.get("PaymentDate") + "", "yyyy-MM-dd'T'HH:mm:ssXXX"),
                    "dd.MM.yyyy HH:mm"
            ));
        } catch (Throwable th) {
            App.error(th);
        }

        BigDecimal sum = new BigDecimal(result.get("Sum") + "");
        BigDecimal fee = new BigDecimal(result.get("SumFee") + "");

        //TODO: надо уточнить какого чёрта
        result.put("AmountString", FwMoneyUtils.num2str(sum.add(fee), CURRENCY_RUB));

        result.put("Sum", String.format("%.2f", sum.doubleValue()));
        result.put("SumFee", String.format("%.2f", fee.doubleValue()));
        result.put("TotalSum", String.format("%.2f", sum.add(fee).doubleValue()));
        result.put("PAYORDER_CONV_RECKPP", "0");
        result.put("C_PAY_CONFIRM_DATE", Util.getDate("dd.MM.yyyy HH:mm"));
        result.put("C_PAY_CONFIRM_PAYMETH_TYPE", mapOper.get(result.get("PayMethodCode")));
        result.put("A_PAYMETH_TXT", mapType.get(result.get("PayMethodCode")));


        String star = "*";
        result.put("FIO", UtilHide.explodeLetterAndMask((String) result.get("FIO"), 2,4,30, star).replace(" ", "  "));
        result.put("Nazn", UtilHide.explodeLetterAndMask((String) result.get("Nazn"), 1,4,40, star));

        return result;
    }

}
