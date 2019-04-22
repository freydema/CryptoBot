package com.freydema.cryptobot.domain;

import static com.freydema.cryptobot.domain.Currency.*;

public enum CurrencyPair {

    BTCEUR(BTC, EUR),
    ETHEUR(ETH, EUR),
    XRPEUR(XRP, EUR),
    BCHEUR(BCH, EUR),
    LTCEUR(LTC, EUR),
    ADAPEUR(ADA, EUR),
    EOSEUR(EOS, EUR),
    REPEUR(REP, EUR),
    XLMEUR(XLM, EUR),
    BSVEUR(BSV, EUR),
    XMREUR(XMR, EUR),
    QTUMEUR(QTUM, EUR),
    ETCEUR(ETC, EUR),
    ZECEUR(ZEC, EUR),
    DASHEUR(DASH, EUR),
    GNOEUR(GNO, EUR);

    private Currency baseCurrency;
    private Currency quoteCurrency;

    CurrencyPair(Currency baseCurrency, Currency quoteCurrency) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
    }

    public Currency getBase() {
        return baseCurrency;
    }

    public Currency getQuote() {
        return quoteCurrency;
    }
}
