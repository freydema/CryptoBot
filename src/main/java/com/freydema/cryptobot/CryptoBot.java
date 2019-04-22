package com.freydema.cryptobot;

import com.freydema.cryptobot.domain.Account;
import com.freydema.cryptobot.domain.Currency;
import com.freydema.cryptobot.domain.CurrencyPair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CryptoBot {



    public void start(){
        CurrencyPair[] pairs = CurrencyPair.values();
        List<CurrencyPairTrader> traders = new ArrayList<>();
        Account account = new Account();
        ExchangeClient exchangeClient = new KrakenExchangeClient(pairs);
        for(CurrencyPair pair : pairs){
            Configuration configuration = Configuration.builder()
                    .currencyPair(pair)
                    .askPriceVsLast24HLowTriggerRatio(BigDecimal.valueOf(0.2))
                    .targetPriceGrowthPercentage(BigDecimal.ONE)
                    .targetRoundTripProfitInEUR(BigDecimal.TEN)
                    .tradeFeePercentage(BigDecimal.valueOf(0.26))
                    .build();
            account.addAsset(pair.getBase(), BigDecimal.ZERO);
            traders.add(new CurrencyPairTrader(configuration, account, exchangeClient));
        }
        account.addAsset(Currency.EUR, BigDecimal.valueOf(10000));
        while(true){
            try {
                Thread.sleep(3000);
                for(CurrencyPairTrader trader : traders){
                    trader.update();
                }
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }


    }

}
