package com.grash.dto.fastSpring;

import lombok.Data;

@Data
public class Instruction {
    public String product;
    public String type;
    public long periodStartDate;
    public long periodStartDateValue;
    public int periodStartDateInSeconds;
    public String periodStartDateDisplay;
    public String periodStartDateDisplayISO8601;
    public Object periodEndDate;
    public Object periodEndDateValue;
    public Object periodEndDateInSeconds;
    public Object periodEndDateDisplay;
    public Object periodEndDateDisplayISO8601;
    public String intervalUnit;
    public int intervalLength;
    public int discountPercent;
    public int discountPercentValue;
    public String discountPercentDisplay;
    public int discountTotal;
    public String discountTotalDisplay;
    public int discountTotalInPayoutCurrency;
    public String discountTotalInPayoutCurrencyDisplay;
    public int unitDiscount;
    public String unitDiscountDisplay;
    public int unitDiscountInPayoutCurrency;
    public String unitDiscountInPayoutCurrencyDisplay;
    public int price;
    public String priceDisplay;
    public int priceInPayoutCurrency;
    public String priceInPayoutCurrencyDisplay;
    public int priceTotal;
    public String priceTotalDisplay;
    public int priceTotalInPayoutCurrency;
    public String priceTotalInPayoutCurrencyDisplay;
    public int unitPrice;
    public String unitPriceDisplay;
    public int unitPriceInPayoutCurrency;
    public String unitPriceInPayoutCurrencyDisplay;
    public int total;
    public String totalDisplay;
    public int totalInPayoutCurrency;
    public String totalInPayoutCurrencyDisplay;
}
