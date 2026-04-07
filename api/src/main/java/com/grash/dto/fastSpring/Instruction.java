package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring subscription instruction with pricing and period details")
public class Instruction {
    @Schema(description = "Product identifier")
    public String product;
    @Schema(description = "Instruction type")
    public String type;
    @Schema(description = "Period start date as long value")
    public long periodStartDate;
    @Schema(description = "Period start date value")
    public long periodStartDateValue;
    @Schema(description = "Period start date in seconds since epoch")
    public int periodStartDateInSeconds;
    @Schema(description = "Formatted period start date")
    public String periodStartDateDisplay;
    @Schema(description = "Period start date in ISO 8601 format")
    public String periodStartDateDisplayISO8601;
    @Schema(description = "Period end date")
    public Object periodEndDate;
    @Schema(description = "Period end date value")
    public Object periodEndDateValue;
    @Schema(description = "Period end date in seconds since epoch")
    public Object periodEndDateInSeconds;
    @Schema(description = "Formatted period end date")
    public Object periodEndDateDisplay;
    @Schema(description = "Period end date in ISO 8601 format")
    public Object periodEndDateDisplayISO8601;
    @Schema(description = "Time unit for the billing interval")
    public String intervalUnit;
    @Schema(description = "Length of the billing interval")
    public int intervalLength;
    @Schema(description = "Discount percentage")
    public int discountPercent;
    @Schema(description = "Discount percentage value")
    public int discountPercentValue;
    @Schema(description = "Formatted discount percentage")
    public String discountPercentDisplay;
    @Schema(description = "Total discount amount")
    public int discountTotal;
    @Schema(description = "Formatted total discount")
    public String discountTotalDisplay;
    @Schema(description = "Total discount in payout currency")
    public int discountTotalInPayoutCurrency;
    @Schema(description = "Formatted total discount in payout currency")
    public String discountTotalInPayoutCurrencyDisplay;
    @Schema(description = "Per-unit discount amount")
    public int unitDiscount;
    @Schema(description = "Formatted per-unit discount")
    public String unitDiscountDisplay;
    @Schema(description = "Per-unit discount in payout currency")
    public int unitDiscountInPayoutCurrency;
    @Schema(description = "Formatted per-unit discount in payout currency")
    public String unitDiscountInPayoutCurrencyDisplay;
    @Schema(description = "Unit price amount")
    public int price;
    @Schema(description = "Formatted unit price")
    public String priceDisplay;
    @Schema(description = "Unit price in payout currency")
    public int priceInPayoutCurrency;
    @Schema(description = "Formatted unit price in payout currency")
    public String priceInPayoutCurrencyDisplay;
    @Schema(description = "Total price amount")
    public int priceTotal;
    @Schema(description = "Formatted total price")
    public String priceTotalDisplay;
    @Schema(description = "Total price in payout currency")
    public int priceTotalInPayoutCurrency;
    @Schema(description = "Formatted total price in payout currency")
    public String priceTotalInPayoutCurrencyDisplay;
    @Schema(description = "Per-unit price amount")
    public int unitPrice;
    @Schema(description = "Formatted per-unit price")
    public String unitPriceDisplay;
    @Schema(description = "Per-unit price in payout currency")
    public int unitPriceInPayoutCurrency;
    @Schema(description = "Formatted per-unit price in payout currency")
    public String unitPriceInPayoutCurrencyDisplay;
    @Schema(description = "Total amount")
    public int total;
    @Schema(description = "Formatted total amount")
    public String totalDisplay;
    @Schema(description = "Total amount in payout currency")
    public int totalInPayoutCurrency;
    @Schema(description = "Formatted total amount in payout currency")
    public String totalInPayoutCurrencyDisplay;
}
