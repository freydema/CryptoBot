package com.freydema.cryptobot;

import com.freydema.cryptobot.domain.CurrencyPair;
import com.freydema.cryptobot.domain.Ticker;

public interface ExchangeClient {

    Ticker getTicker(CurrencyPair pair);


}
