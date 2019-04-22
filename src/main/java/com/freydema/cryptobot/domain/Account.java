package com.freydema.cryptobot.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Account {

    private static final Logger LOGGER = LoggerFactory.getLogger(Account.class);

    private Map<Currency, BigDecimal> balance;
    private Map<Currency, BigDecimal> blocked;

    public Account() {
        balance = new HashMap<>();
        blocked = new HashMap<>();
    }

    public void addAsset(Currency currency, BigDecimal amount) {
        BigDecimal currentAmount = balance.get(currency);
        if(currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
        balance.put(currency, currentAmount.add(amount));
    }

    public void removeAsset(Currency currency, BigDecimal amount) {
        BigDecimal currentAmount = balance.get(currency);
        currentAmount = currentAmount.subtract(amount);
//        if(currentAmount.signum() < 0){
//            currentAmount = BigDecimal.ZERO;
//        }
        balance.put(currency, currentAmount);
    }

    public void blockAsset(Currency currency, BigDecimal amount) {
        BigDecimal currentAmount = blocked.get(currency);
        if(currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
        blocked.put(currency, currentAmount.add(amount));
    }

    public void unblockAsset(Currency currency, BigDecimal amount) {
        BigDecimal currentAmount = blocked.get(currency);
        currentAmount = currentAmount.subtract(amount);
        if(currentAmount.signum() < 0){
            currentAmount = BigDecimal.ZERO;
        }
        blocked.put(currency, currentAmount);
    }

    public BigDecimal getBalance(Currency currency) {
        BigDecimal amount = balance.get(currency);
        return amount != null ? amount: BigDecimal.ZERO;
    }

    public BigDecimal getBlocked(Currency currency) {
        BigDecimal amount = blocked.get(currency);
        return amount != null ? amount: BigDecimal.ZERO;
    }

}
