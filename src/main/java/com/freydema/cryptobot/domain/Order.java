package com.freydema.cryptobot.domain;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @EqualsAndHashCode.Include
    private final String id;

    private final CurrencyPair pair;
    private final BigDecimal quantity;
    private final BigDecimal limit;
    private final OrderSide side;
    private final LocalDateTime createdAt;

    @Builder
    private Order(String id, CurrencyPair pair, BigDecimal quantity, BigDecimal limit, OrderSide side) {
        this.id = id;
        this.pair = pair;
        this.quantity = quantity;
        this.limit = limit;
        this.side = side;
        createdAt = LocalDateTime.now();
    }
}
