package com.freydema.cryptobot;

import com.freydema.cryptobot.domain.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;
import static com.freydema.cryptobot.BigDecimalUtils.*;

public class CurrencyPairTraderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyPairTraderTest.class);
    private static final BigDecimal EPSILON = BigDecimal.valueOf(0.0001);

    private Account account;
    private Queue<Ticker> tickerQueue;
    private CurrencyPairTrader trader;

    @Before
    public void setup() {
        Configuration configuration = Configuration.builder()
                .currencyPair(CurrencyPair.BTCEUR)
                .askPriceVsLast24HLowTriggerRatio(BigDecimal.valueOf(0.2))
                .targetPriceGrowthPercentage(BigDecimal.ONE)
                .targetRoundTripProfitInEUR(BigDecimal.TEN)
                .tradeFeePercentage(BigDecimal.valueOf(0.26))
                .build();
        account = new Account();
        tickerQueue = new LinkedList<>();
        ExchangeClient exchangeClient = new TestExchangeClient(tickerQueue);
        trader = new CurrencyPairTrader(configuration, account, exchangeClient);
    }


    @Test
    public void test() {

        // Setup test data
        account.addAsset(Currency.EUR, BigDecimal.valueOf(5000));
        account.addAsset(Currency.BTC, BigDecimal.ZERO);

        // Validate setup
        Assert.assertEquals(CurrencyPair.BTCEUR, trader.getPair());
        Assert.assertEquals(CurrencyPairTrader.State.START, trader.getState());
        Assert.assertNull(trader.getBuyOrder());
        Assert.assertNull(trader.getSellOrder());
        Assert.assertEquals(BigDecimal.valueOf(5000), trader.getAccount().getBalance(Currency.EUR));
        Assert.assertEquals(BigDecimal.valueOf(0), trader.getAccount().getBalance(Currency.BTC));

        // Recover
        trader.recover();
        Assert.assertEquals(CurrencyPairTrader.State.TRY_BUY, trader.getState());

        // Trade

        // Ticker 1: should not buy
        tickerQueue.offer(Ticker.of(12.1, 12.2, 10, 20));
        trader.update();
        Assert.assertEquals(CurrencyPairTrader.State.TRY_BUY, trader.getState());
        Assert.assertNull(trader.getBuyOrder());

        // Ticker 2: should buy
        tickerQueue.offer(Ticker.of(11.9, 12, 10, 20));
        trader.update();
        Assert.assertEquals(CurrencyPairTrader.State.WAIT_FOR_BUY_ORDER_EXECUTED, trader.getState());
        Order buyOrder = trader.getBuyOrder();
        Assert.assertNotNull(buyOrder);
        Assert.assertEquals(CurrencyPair.BTCEUR, buyOrder.getPair());
        Assert.assertEquals(OrderSide.BUY, buyOrder.getSide());
        Assert.assertEquals(BigDecimal.valueOf(174.557), buyOrder.getQuantity());
        Assert.assertEquals(BigDecimal.valueOf(12.0), buyOrder.getLimit());

        // Ticker 3: buy order should not be executed
        tickerQueue.offer(Ticker.of(11.9, 12, 10, 20));
        trader.update();
        Assert.assertEquals(CurrencyPairTrader.State.WAIT_FOR_BUY_ORDER_EXECUTED, trader.getState());

        // Ticker 4: buy order should be executed
        tickerQueue.offer(Ticker.of(12, 12.1, 10, 20));
        trader.update();
        Assert.assertEquals(CurrencyPairTrader.State.TRY_SELL, trader.getState());

        // Next update: sell order should be placed
        trader.update();
        Assert.assertEquals(CurrencyPairTrader.State.WAIT_FOR_SELL_ORDER_EXECUTED, trader.getState());
        Order sellOrder = trader.getSellOrder();
        Assert.assertNotNull(sellOrder);
        Assert.assertEquals(CurrencyPair.BTCEUR, sellOrder.getPair());
        Assert.assertEquals(OrderSide.SELL, sellOrder.getSide());
        Assert.assertEquals(BigDecimal.valueOf(174.557), sellOrder.getQuantity());
        //Assert.assertEquals(BigDecimal.valueOf(12.120), sellOrder.getLimit());

        // Ticker 5: sell order should be executed
        tickerQueue.offer(Ticker.of(12, 12.1, 10, 20));
        trader.update();

        Assert.assertEquals(CurrencyPairTrader.State.END_ROUND_TRIP, trader.getState());


    }

    @Test
    public void shouldBuyGivenTickerTest() {
        // Below ratio
        Ticker ticker = Ticker.of(11, 11, 10, 20);
        Assert.assertTrue(trader.shouldBuyGivenTicker(ticker));
        // At ratio
        ticker = Ticker.of(12, 12, 10, 20);
        Assert.assertTrue(trader.shouldBuyGivenTicker(ticker));
        // Above ratio
        ticker = Ticker.of(12.1, 12.1, 10, 20);
        Assert.assertFalse(trader.shouldBuyGivenTicker(ticker));
    }

    @Test
    public void calculateBuyQuantityGivenTest() {
        // Test case 1
        BigDecimal targetProfit = BigDecimal.valueOf(10);
        BigDecimal targetPriceMovePercentage = BigDecimal.ONE;
        BigDecimal feePercentage = BigDecimal.valueOf(0.25);
        BigDecimal buyPrice = BigDecimal.valueOf(100);
        BigDecimal calculatedQuantity = CurrencyPairTrader.calculateBuyQuantityGiven(buyPrice, targetProfit, targetPriceMovePercentage, feePercentage);
        Assert.assertTrue(calculatedQuantity.signum() > 0);
        Assert.assertEquals(BigDecimal.valueOf(20.1005), calculatedQuantity);
        BigDecimal actualProfit = calculateActualProfit(buyPrice, targetPriceMovePercentage, calculatedQuantity, feePercentage);
        Assert.assertTrue(targetProfit.subtract(actualProfit).abs().compareTo(EPSILON) < 0);
        // Test case 2
        targetProfit = BigDecimal.valueOf(10);
        targetPriceMovePercentage = BigDecimal.valueOf(1);
        feePercentage = BigDecimal.valueOf(0.26);
        buyPrice = BigDecimal.valueOf(0.3);
        calculatedQuantity = CurrencyPairTrader.calculateBuyQuantityGiven(buyPrice, targetProfit, targetPriceMovePercentage, feePercentage).abs();
        Assert.assertTrue(calculatedQuantity.signum() > 0);
        actualProfit = calculateActualProfit(buyPrice, targetPriceMovePercentage, calculatedQuantity, feePercentage);
        Assert.assertTrue(targetProfit.subtract(actualProfit).abs().compareTo(EPSILON) < 0);
    }

    private BigDecimal calculateActualProfit(BigDecimal buyPrice,
                                            BigDecimal targetPriceMovePercentage,
                                            BigDecimal quantity,
                                            BigDecimal feePercentage) {
        BigDecimal buyAmount = buyPrice.multiply(quantity);
        BigDecimal buyFee = buyPrice.multiply(quantity).multiply(feePercentage).divide(HUNDRED);
        BigDecimal sellPrice = buyPrice.multiply(HUNDRED.add(targetPriceMovePercentage).divide(HUNDRED));
        BigDecimal sellAmount = sellPrice.multiply(quantity);
        BigDecimal sellFee = sellPrice.multiply(quantity).multiply(feePercentage).divide(HUNDRED);
        BigDecimal actualProfit = sellPrice.multiply(quantity)
                .subtract(buyPrice.multiply(quantity))
                .subtract(buyFee)
                .subtract(sellFee);
        LOGGER.info("Checking the actual profit with the calculated BUY quantity: {}", quantity);
        LOGGER.info("Buy price = {} ", buyPrice);
        LOGGER.info("Buy amount = {} ", buyAmount);
        LOGGER.info("Buy fee = {}", buyFee);
        LOGGER.info("Sell price = {}", sellPrice);
        LOGGER.info("Sell amount = {} ", sellAmount);
        LOGGER.info("Sell fee = {}", sellFee);
        LOGGER.info("Actual profit = {}", actualProfit);
        return actualProfit;
    }


    private static class TestExchangeClient implements ExchangeClient {

        private final Queue<Ticker> tickers;


        public TestExchangeClient(Queue<Ticker> tickers) {
            this.tickers = tickers;
        }

        @Override
        public Ticker getTicker(CurrencyPair pair) {
            return tickers.poll();
        }

    }



}
