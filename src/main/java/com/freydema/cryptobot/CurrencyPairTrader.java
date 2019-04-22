package com.freydema.cryptobot;

import com.freydema.cryptobot.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.UUID;
import static com.freydema.cryptobot.BigDecimalUtils.*;

public class CurrencyPairTrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyPairTrader.class);


    public enum State {
        START,
        TRY_BUY,
        //WAIT_FOR_BUY_ORDER_PLACED,
        WAIT_FOR_BUY_ORDER_EXECUTED,
        TRY_SELL,
        //WAIT_FOR_SELL_ORDER_PLACED,
        WAIT_FOR_SELL_ORDER_EXECUTED,
        END_ROUND_TRIP,
        STOP
    }


    private final Configuration config;
    private final Account account;
    private final ExchangeClient exchangeClient;
    private final CurrencyPair pair;

    private State state;
    private Order buyOrder;
    private Order sellOrder;


    public CurrencyPairTrader(Configuration config, Account account, ExchangeClient exchangeClient) {
        this.config = config;
        this.account = account;
        this.exchangeClient = exchangeClient;
        this.pair = config.getCurrencyPair();
        this.state = State.START;
        config.validate();
        // Check config
        if(config.getTargetPriceGrowthPercentage().compareTo(config.getTradeFeePercentage().multiply(TWO)) <= 0){
            throw new RuntimeException("Invalid config: Target Price Growth Percentage <=  2x TradeFee");
        }
    }

    public void recover() {
        moveToState(State.TRY_BUY);
    }


    public CurrencyPair getPair() {
        return pair;
    }

    public Account getAccount() {
        return account;
    }

    public State getState() {
        return state;
    }

    public Order getBuyOrder() {
        return buyOrder;
    }

    public Order getSellOrder() {
        return sellOrder;
    }

    protected void update() {

        switch (state) {
            case START:
                // do nothing
                break;
            case TRY_BUY:
                tryBuy();
                break;
//            case WAIT_FOR_BUY_ORDER_PLACED:
//                waitForBuyOrderPlaced();
//                break;
            case WAIT_FOR_BUY_ORDER_EXECUTED:
                waitForBuyOrderExecuted();
                break;
            case TRY_SELL:
                trySell();
                break;
//            case WAIT_FOR_SELL_ORDER_PLACED:
//                waitForSellOrderPlaced();
//                break;
            case WAIT_FOR_SELL_ORDER_EXECUTED:
                waitForSellOrderExecuted();
                break;
            case END_ROUND_TRIP:
                endRoundTrip();
                break;
            case STOP:
                // do nothing
                break;
        }
    }

    private void tryBuy() {
        BigDecimal balance = account.getBalance(pair.getQuote());
        BigDecimal blocked = account.getBlocked(pair.getQuote());
        BigDecimal available = balance.subtract(blocked);
        if(available.compareTo(BigDecimal.ZERO) == 0){
            LOGGER.info("No available fund for trading left");
            moveToState(State.STOP);
        }
        Ticker ticker = exchangeClient.getTicker(pair);
        if(shouldBuyGivenTicker(ticker)) {
           BigDecimal buyPrice = ticker.getAskPrice();
           BigDecimal quantity = calculateBuyQuantityGiven(
                   buyPrice,
                   config.getTargetRoundTripProfitInEUR(),
                   config.getTargetPriceGrowthPercentage(),
                   config.getTradeFeePercentage());
           buyOrder = Order.builder()
                   .id(UUID.randomUUID().toString())
                   .pair(pair)
                   .side(OrderSide.BUY)
                   .quantity(quantity)
                   .limit(buyPrice)
                   .build();
           LOGGER.info("Placed BUY {}", buyOrder);
           BigDecimal blockedAmount = buyPrice.multiply(quantity);
           account.blockAsset(pair.getQuote(), blockedAmount);
           LOGGER.info("Blocked {} {}", blockedAmount, pair.getQuote());
           printAccountBalance();
           moveToState(State.WAIT_FOR_BUY_ORDER_EXECUTED);
        }
    }

    protected boolean shouldBuyGivenTicker(Ticker ticker){
        BigDecimal askPrice = ticker.getAskPrice();
        BigDecimal last24HLow = ticker.getLast24HLow();
        BigDecimal last24HHigh = ticker.getLast24HHigh();
        BigDecimal last24HDelta = last24HHigh.subtract(last24HLow);
        BigDecimal askPriceDelta = askPrice.subtract(last24HLow);
        BigDecimal ratio = askPriceDelta.divide(last24HDelta);
        return ratio.compareTo(config.getAskPriceVsLast24HLowTriggerRatio()) <= 0;
    }

    protected static BigDecimal calculateBuyQuantityGiven(BigDecimal buyPrice,
                                                          BigDecimal targetProfit,
                                                          BigDecimal targetPriceMovePercentage,
                                                          BigDecimal feePercentage){
        LOGGER.info("Calculating BUY quantity given: buyPrice={}, targetProfit={}, targetPriceMove={}%, fee={}%",
                buyPrice, targetProfit, targetPriceMovePercentage, feePercentage);
        BigDecimal f = feePercentage.divide(HUNDRED);
        BigDecimal k = HUNDRED.add(targetPriceMovePercentage).divide(HUNDRED);
        /*

            If k < (1+f)/1-f) then the calculated quantity will be systematically negative.
            That's because it's a necessary condition to meet the targetProfit requirement.
            Meaning that will would be systematically making loss if that same quantity would be positive

            Therefore we MUST choose k and f so that K > (1+f)/(1-f)  !!!

            If you think about it that's because if we have for example 0.25% of fee (f)  we sell after the price has
            moved by +0.2% (k) then the fees payed (2x 0.25%) are greater than the price move. So we lose money in fees

         */

        // q = targetProfit / buyPrice(k(1-f)-(1+f))
        BigDecimal q = targetProfit
                .divide(
                    buyPrice.multiply(
                        k.multiply(ONE.subtract(f)).subtract(ONE.add(f))
                    ), new MathContext(6, RoundingMode.HALF_EVEN)
                );
        LOGGER.info("BUY quantity = {}", q);
        return q;
    }

    private void waitForBuyOrderExecuted() {
        Ticker ticker = exchangeClient.getTicker(pair);
        BigDecimal buyOrderLimit = buyOrder.getLimit();
        if(ticker.getAskPrice().compareTo(buyOrderLimit) > 0){
            LOGGER.info("BUY order limit= {} vs ticker ask price= {} => assuming BUY order executed", buyOrderLimit, ticker.getAskPrice());
            // If the current ask price is above the buyOrder limit, assume that the order was executed
            // TODO introduce the execution price. For now Assume order has been executed at the order limit price
            //  exactly (in real the executed price could be different)
            BigDecimal executedAmount = buyOrder.getQuantity().multiply(buyOrderLimit); // NOT CORRECT
            BigDecimal blockedAmount = buyOrder.getQuantity().multiply(buyOrderLimit); // CORRECT
            BigDecimal fee = executedAmount.multiply(config.getTradeFeePercentage()).divide(HUNDRED);
            BigDecimal buyCost = executedAmount.add(fee);
            account.unblockAsset(pair.getQuote(), blockedAmount);
            account.removeAsset(pair.getQuote(), buyCost);
            account.addAsset(pair.getBase(), buyOrder.getQuantity());
            LOGGER.info("BUY fee = {} {}", fee, pair.getQuote());
            LOGGER.info("Unblocked {} {}", blockedAmount, pair.getQuote());
            LOGGER.info("Removed {} {}", buyCost, pair.getQuote());
            LOGGER.info("Added {} {}", buyOrder.getQuantity(), pair.getBase());
            printAccountBalance();
            moveToState(State.TRY_SELL);
        }
    }

    private void trySell() {
        BigDecimal sellPrice = buyOrder.getLimit().multiply(HUNDRED.add(
                config.getTargetPriceGrowthPercentage()).divide(HUNDRED));
        BigDecimal quantity = account.getBalance(pair.getBase());
        sellOrder = Order.builder()
                .id(UUID.randomUUID().toString())
                .pair(pair)
                .side(OrderSide.SELL)
                .quantity(quantity)
                .limit(sellPrice)
                .build();
        LOGGER.info("Placed SELL {}", sellOrder);
        //BigDecimal blockedAmount = sellPrice.multiply(quantity);
        account.blockAsset(pair.getBase(), quantity);
        LOGGER.info("Blocked {} {}", quantity, pair.getBase());
        moveToState(State.WAIT_FOR_SELL_ORDER_EXECUTED);
    }

    private void waitForSellOrderExecuted() {
        Ticker ticker = exchangeClient.getTicker(pair);
        BigDecimal sellOrderLimit = sellOrder.getLimit();
        if(ticker.getBidPrice().compareTo(sellOrderLimit) < 0){
            LOGGER.info("SELL order limit= {} vs ticker bid price= {} => assuming SELL order executed", sellOrderLimit, ticker.getBidPrice());
            // If the current bid price is below the sellOrder limit, assume that the order was executed
            // TODO introduce the execution price. For now Assume order has been executed at the order limit price
            //  exactly (in real the executed price could be different)
            BigDecimal executedAmount = sellOrder.getQuantity(); // NOT CORRECT
            BigDecimal blockedAmount = sellOrder.getQuantity(); // CORRECT
            BigDecimal fee = executedAmount.multiply(sellOrder.getLimit()).multiply(config.getTradeFeePercentage()).divide(HUNDRED);
            BigDecimal quoteCurrencyAmount = executedAmount.multiply(sellOrder.getLimit()).subtract(fee);
            account.unblockAsset(pair.getBase(), blockedAmount);
            account.removeAsset(pair.getBase(), executedAmount);
            account.addAsset(pair.getQuote(), quoteCurrencyAmount);
            LOGGER.info("SELL fee = {} {}", fee, pair.getQuote());
            LOGGER.info("Unblocked {} {}", blockedAmount, pair.getBase());
            LOGGER.info("Removed {} {} ", executedAmount, pair.getBase());
            LOGGER.info("Added {} {} (fee = {} {}}", quoteCurrencyAmount, pair.getQuote(), fee, pair.getQuote());
            printAccountBalance();
            moveToState(State.END_ROUND_TRIP);
        }
    }

    private void endRoundTrip() {
        buyOrder = null;
        sellOrder = null;
        moveToState(State.TRY_BUY);
    }

    private void moveToState(State newState){
        LOGGER.info("{} -> {}", state, newState);
        state = newState;
    }

    private void printAccountBalance(){
        LOGGER.info("Account balance {}: {} ({} blocked), {}: {} ({} blocked)",
                pair.getQuote(), account.getBalance(pair.getQuote()), account.getBlocked(pair.getQuote()),
                pair.getBase(), account.getBalance(pair.getBase()), account.getBlocked(pair.getBase())
        );
    }

}
