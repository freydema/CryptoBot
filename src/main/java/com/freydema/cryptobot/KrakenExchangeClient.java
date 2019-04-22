package com.freydema.cryptobot;

import com.freydema.cryptobot.domain.CurrencyPair;
import com.freydema.cryptobot.domain.Ticker;

public class KrakenExchangeClient implements ExchangeClient{

    public KrakenExchangeClient(CurrencyPair[] pairs) {
    }


    @Override
    public Ticker getTicker(CurrencyPair pair) {
        return null;
    }
}
