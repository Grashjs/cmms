package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring order line item")
public class Item {
    @Schema(description = "Product identifier")
    public String product;
    @Schema(description = "Item quantity")
    public int quantity;
    @Schema(description = "Item display name")
    public String display;
    @Schema(description = "Stock keeping unit (SKU)")
    public Object sku;
    @Schema(description = "Item image URL")
    public Object imageUrl;
    @Schema(description = "Item subtotal amount")
    public int subtotal;
    @Schema(description = "Formatted subtotal")
    public String subtotalDisplay;
    @Schema(description = "Subtotal in payout currency")
    public int subtotalInPayoutCurrency;
    @Schema(description = "Formatted subtotal in payout currency")
    public String subtotalInPayoutCurrencyDisplay;
    @Schema(description = "Discount amount applied")
    public int discount;
    @Schema(description = "Formatted discount amount")
    public String discountDisplay;
    @Schema(description = "Discount in payout currency")
    public int discountInPayoutCurrency;
    @Schema(description = "Formatted discount in payout currency")
    public String discountInPayoutCurrencyDisplay;
    @Schema(description = "Subscription details for this item")
    public Subscription subscription;
    @Schema(description = "Fulfillment information for this item")
    public Fulfillments fulfillments;
    @Schema(description = "Tax withholding information for this item")
    public Withholdings withholdings;
}
