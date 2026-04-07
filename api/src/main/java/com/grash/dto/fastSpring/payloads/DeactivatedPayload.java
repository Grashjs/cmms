package com.grash.dto.fastSpring.payloads;

import com.grash.dto.fastSpring.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;

@Data
@Schema(description = "FastSpring deactivated subscription payload")
public class DeactivatedPayload {
    @Schema(description = "Subscription identifier")
    public String id;
    @Schema(description = "Quote reference")
    public String quote;
    @Schema(description = "Subscription reference number")
    public String subscription;
    @Schema(description = "Whether the subscription is currently active")
    public boolean active;
    @Schema(description = "Current state of the subscription")
    public String state;
    @Schema(description = "Timestamp when the subscription was last changed")
    public long changed;
    @Schema(description = "Changed date value")
    public long changedValue;
    @Schema(description = "Changed date in seconds since epoch")
    public int changedInSeconds;
    @Schema(description = "Formatted changed date display")
    public String changedDisplay;
    @Schema(description = "Whether this is a live subscription (vs sandbox)")
    public boolean live;
    @Schema(description = "Currency code for the subscription")
    public String currency;
    @Schema(description = "Account reference for this subscription")
    public String account;
    @Schema(description = "Product identifier for this subscription")
    public String product;
    @Schema(description = "Stock keeping unit (SKU)")
    public Object sku;
    @Schema(description = "Display name for the subscription")
    public String display;
    @Schema(description = "Subscription quantity")
    public int quantity;
    @Schema(description = "Whether this is an ad-hoc subscription")
    public boolean adhoc;
    @Schema(description = "Whether auto-renewal is enabled")
    public boolean autoRenew;
    @Schema(description = "Subscription price amount")
    public int price;
    @Schema(description = "Formatted price display")
    public String priceDisplay;
    @Schema(description = "Price in payout currency")
    public int priceInPayoutCurrency;
    @Schema(description = "Formatted price in payout currency")
    public String priceInPayoutCurrencyDisplay;
    @Schema(description = "Discount amount applied")
    public int discount;
    @Schema(description = "Formatted discount display")
    public String discountDisplay;
    @Schema(description = "Discount in payout currency")
    public int discountInPayoutCurrency;
    @Schema(description = "Formatted discount in payout currency")
    public String discountInPayoutCurrencyDisplay;
    @Schema(description = "Subtotal amount before discount")
    public int subtotal;
    @Schema(description = "Formatted subtotal display")
    public String subtotalDisplay;
    @Schema(description = "Subtotal in payout currency")
    public int subtotalInPayoutCurrency;
    @Schema(description = "Formatted subtotal in payout currency")
    public String subtotalInPayoutCurrencyDisplay;
    @Schema(description = "Next billing date timestamp")
    public long next;
    @Schema(description = "Next billing date value")
    public long nextValue;
    @Schema(description = "Next billing date in seconds since epoch")
    public int nextInSeconds;
    @Schema(description = "Formatted next billing date")
    public String nextDisplay;
    @Schema(description = "Subscription end date timestamp")
    public long end;
    @Schema(description = "End date value")
    public long endValue;
    @Schema(description = "End date in seconds since epoch")
    public int endInSeconds;
    @Schema(description = "Formatted end date")
    public String endDisplay;
    @Schema(description = "Cancellation date timestamp")
    public long canceledDate;
    @Schema(description = "Cancellation date value")
    public long canceledDateValue;
    @Schema(description = "Cancellation date in seconds since epoch")
    public int canceledDateInSeconds;
    @Schema(description = "Formatted cancellation date")
    public String canceledDateDisplay;
    @Schema(description = "Deactivation date timestamp")
    public Long deactivationDate;
    @Schema(description = "Deactivation date value")
    public Long deactivationDateValue;
    @Schema(description = "Deactivation date in seconds since epoch")
    public Long deactivationDateInSeconds;
    @Schema(description = "Formatted deactivation date")
    public String deactivationDateDisplay;
    @Schema(description = "Billing sequence number")
    public int sequence;
    @Schema(description = "Total number of billing periods")
    public int periods;
    @Schema(description = "Remaining billing periods")
    public int remainingPeriods;
    @Schema(description = "Subscription begin date timestamp")
    public long begin;
    @Schema(description = "Begin date value")
    public long beginValue;
    @Schema(description = "Begin date in seconds since epoch")
    public int beginInSeconds;
    @Schema(description = "Formatted begin date")
    public String beginDisplay;
    @Schema(description = "Billing interval unit (e.g., month, year)")
    public String intervalUnit;
    @Schema(description = "Length of the billing interval")
    public int intervalLength;
    @Schema(description = "Payment reminder configuration")
    public PaymentReminder paymentReminder;
    @Schema(description = "Payment overdue tracking information")
    public PaymentOverdue paymentOverdue;
    @Schema(description = "Cancellation settings for this subscription")
    public CancellationSetting cancellationSetting;
    @Schema(description = "Fulfillment information for this subscription")
    public Fulfillments fulfillments;
    @Schema(description = "List of billing instructions for this subscription")
    public ArrayList<Instruction> instructions;
}
