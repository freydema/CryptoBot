package com.freydema.cryptobot.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@Getter
@ToString
public class Ticker {

    private BigDecimal askPrice;
    private BigDecimal askVolume;
    private BigDecimal bidPrice;
    private BigDecimal bidVolume;
    private BigDecimal last24HLow;
    private BigDecimal last24HHigh;


    public static Ticker of(double bidPrice, double askPrice, double last24HLow, double last24HHigh){
        return Ticker.builder()
                .askPrice(BigDecimal.valueOf(askPrice))
                .bidPrice(BigDecimal.valueOf(bidPrice))
                .last24HLow(BigDecimal.valueOf(last24HLow))
                .last24HHigh(BigDecimal.valueOf(last24HHigh))
                .build();
    }

    /*

    From Kraken api

     a = ask array(<price>, <whole lot volume>, <lot volume>),
    b = bid array(<price>, <whole lot volume>, <lot volume>),
    c = last trade closed array(<price>, <lot volume>),
    v = volume array(<today>, <last 24 hours>),
    p = volume weighted average price array(<today>, <last 24 hours>),
    t = number of trades array(<today>, <last 24 hours>),
    l = low array(<today>, <last 24 hours>),
    h = high array(<today>, <last 24 hours>),
    o = today's opening price

     */

}
