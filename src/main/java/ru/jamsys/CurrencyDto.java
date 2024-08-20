package ru.jamsys;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class CurrencyDto {

    private String nameCurr;
    private String name1;
    private String name2to4;
    private String name5to20;
    private String fractionalName1;
    private String fractionalName2to4;
    private String fractionalName5to20;

    public CurrencyDto(String nameCurr, String name1, String name2to4, String name5to20, String fractionalName1, String fractionalName2to4, String fractionalName5to20) {
        this.nameCurr = nameCurr;
        this.name1 = name1;
        this.name2to4 = name2to4;
        this.name5to20 = name5to20;
        this.fractionalName1 = fractionalName1;
        this.fractionalName2to4 = fractionalName2to4;
        this.fractionalName5to20 = fractionalName5to20;
    }

    public CurrencyDto() {
    }

    @Override
    public String toString() {
        return "CurrencyDto{" +
                "nameCurr='" + nameCurr + '\'' +
                ", name1='" + name1 + '\'' +
                ", name2to4='" + name2to4 + '\'' +
                ", name5to20='" + name5to20 + '\'' +
                ", fractionalName1='" + fractionalName1 + '\'' +
                ", fractionalName2to4='" + fractionalName2to4 + '\'' +
                ", fractionalName5to20='" + fractionalName5to20 + '\'' +
                '}';
    }
}