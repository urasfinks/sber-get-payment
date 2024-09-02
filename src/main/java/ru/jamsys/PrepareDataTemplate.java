package ru.jamsys;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilHide;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrepareDataTemplate {

    public static void main(String[] args) throws IOException {
        System.out.println(UtilJson.toStringPretty(parse(UtilFileResource.getAsString("data.json")), "{}"));
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
                , result
                , "***");

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
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Util.getTimestamp(result.get("PaymentDate") + "", "yyyy-MM-dd'T'HH:mm:ssXXX") * 1000);
            String[] monthNames = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
            String month = monthNames[cal.get(Calendar.MONTH)];
            result.put("PaymentDatePretty",
                    cal.get(Calendar.DATE)
                            + " " + month
                            + " " + cal.get(Calendar.YEAR)
                            + " " + Util.padLeft(cal.get(Calendar.HOUR_OF_DAY)+"", 2, "0")
                            + ":" + Util.padLeft(cal.get(Calendar.MINUTE)+"", 2, "0")
                            + ":" + Util.padLeft(cal.get(Calendar.SECOND)+"", 2, "0") + " мск"
            );

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
        String s = FwMoneyUtils.num2str(sum.add(fee), CURRENCY_RUB);
        result.put("AmountString", s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());

        result.put("Sum", String.format("%.2f", sum.doubleValue()));
        result.put("SumFee", String.format("%.2f", fee.doubleValue()));
        result.put("TotalSum", String.format("%.2f", sum.add(fee).doubleValue()));
        result.put("KPP", "0");
        result.put("Now", Util.getDate("dd.MM.yyyy HH:mm"));
        result.put("PaymentOperation", mapOper.get(result.get("PayMethodCode")));
        result.put("PaymentType", mapType.get(result.get("PayMethodCode")));

        result.put("Nazn", ufoParse((String) result.get("Nazn"), ":", ";"));

        String star = "*";
        result.put("FIO", UtilHide.explodeLetterAndMask((String) result.get("FIO"), 2,4,30, star).replace(" ", "  "));
        result.remove("PayMethodCode");

        return result;
    }

    public static Map<String, String> ufoParse(String req_ufo, String key_del, String main_del) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] main_split = req_ufo.split(main_del);

        String last_key = "";
        String key = "";
        String value = "";

        for (int i = 0; i < main_split.length; i++) {
            if (main_split[i].indexOf(key_del) <= 0) {
                value = main_split[i];
                key = last_key;
            } else {
                key = main_split[i].substring(0, main_split[i].indexOf(key_del));
                last_key = key;
                value = main_split[i].substring(main_split[i].indexOf(key_del) + 1);
            }
            if (result.containsKey(key)) {
                result.put(key.trim(), (result.get(key) + main_del + value).trim());
            } else {
                result.put(key.trim(), (value).trim());
            }
        }
        result.remove("EXCLUDE_REQS");
        Map<String, String> replaceReqKey = new HashMapBuilder<String, String>()
                .append("fio", "ФИО")
                .append("payername", "ФИО")
                .append("address", "Адрес")
                .append("adress", "Адрес")
                .append("addres", "Адрес")
                .append("adres", "Адрес")
                .append("ls", "Лицевой счёт")
                .append("pacc", "Лицевой счёт")
                .append("persacc", "Лицевой счёт")
                .append("pers_acc", "Лицевой счёт")
                .append("lsc", "Лицевой счёт");
        Map<String, String> ret = new HashMap<>();
        result.forEach((k, v) -> {
            String loweKey = k.toLowerCase();
            v = UtilHide.explodeLetterAndMask(v, 1,4,40, "*");
            if (replaceReqKey.containsKey(loweKey)) {
                ret.put(replaceReqKey.get(loweKey), v);
            } else {
                ret.put(Util.ucword(loweKey), v);
            }
        });
        return ret;
    }

}
