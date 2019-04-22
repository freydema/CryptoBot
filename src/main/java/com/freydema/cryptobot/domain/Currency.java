package com.freydema.cryptobot.domain;

public enum  Currency {

    BTC("Bitcoin"),
    ETH("Ethereum"),
    XRP("Ripple"),
    BCH("Bitcoin Cash"),
    LTC("Litecoin"),
    ADA("Cardano"),
    EOS("EOS"),
    REP("Augur"),
    XLM("Stellar"),
    BSV("BitcoinSV"),
    XMR("Monero"),
    QTUM("Qtum"),
    ETC("Ethereum Classic"),
    ZEC("Zcash"),
    DASH("Dash"),
    GNO("Gnosis"),
    EUR("Euro");

    private String realName;

    Currency(String realName) {
        this.realName = realName;
    }

    public String getRealName() {
        return realName;
    }
}
