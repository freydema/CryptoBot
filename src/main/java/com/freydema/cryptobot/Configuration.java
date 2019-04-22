package com.freydema.cryptobot;

import com.freydema.cryptobot.domain.CurrencyPair;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.math.BigDecimal;
import static com.freydema.cryptobot.BigDecimalUtils.*;

@Builder
@Getter
@ToString
public class Configuration {

    private final CurrencyPair currencyPair;
    private final BigDecimal askPriceVsLast24HLowTriggerRatio;
    private final BigDecimal targetRoundTripProfitInEUR;
    private final BigDecimal targetPriceGrowthPercentage;
    private final BigDecimal tradeFeePercentage;


    public void validate() {
        if(targetPriceGrowthPercentage.compareTo(tradeFeePercentage.multiply(TWO)) <= 0){
            throw new RuntimeException("Invalid configuration: Target Price Growth Percentage <=  2x TradeFee");
        }
    }
}
