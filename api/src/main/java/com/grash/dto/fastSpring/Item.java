package com.grash.dto.fastSpring;

import lombok.Data;

@Data
public class Item {
    public String product;
    public int quantity;
    public String display;
    public Object sku;
    public Object imageUrl;
    public int subtotal;
    public String subtotalDisplay;
    public int subtotalInPayoutCurrency;
    public String subtotalInPayoutCurrencyDisplay;
    public int discount;
    public String discountDisplay;
    public int discountInPayoutCurrency;
    public String discountInPayoutCurrencyDisplay;
    public Subscription subscription;
    public Fulfillments fulfillments;
    public Withholdings withholdings;
}
