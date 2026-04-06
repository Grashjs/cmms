package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;

@Data
@Schema(description = "FastSpring subscription details")
public class Subscription {
    @Schema(description = "Subscription identifier")
    public String id;
    @Schema(description = "Quote reference for the subscription")
    public Object quote;
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
    @Schema(description = "Changed date in ISO 8601 format")
    public String changedDisplayISO8601;
    @Schema(description = "Whether this is a live subscription (vs sandbox)")
    public boolean live;
    @Schema(description = "Currency code for the subscription")
    public String currency;
    @Schema(description = "Account reference for this subscription")
    public String account;
    @Schema(description = "Product identifier for this subscription")
    public String product;
    @Schema(description = "Stock keeping unit (SKU) for the subscription")
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
    @Schema(description = "Tags associated with the subscription")
    public Tags tags;
    @Schema(description = "Next billing date timestamp")
    public long next;
    @Schema(description = "Next billing date value")
    public long nextValue;
    @Schema(description = "Next billing date in seconds since epoch")
    public int nextInSeconds;
    @Schema(description = "Formatted next billing date")
    public String nextDisplay;
    @Schema(description = "Next billing date in ISO 8601 format")
    public String nextDisplayISO8601;
    @Schema(description = "Subscription end date timestamp")
    public Long end;
    @Schema(description = "End date value")
    public Long endValue;
    @Schema(description = "End date in seconds since epoch")
    public Long endInSeconds;
    @Schema(description = "Formatted end date")
    public String endDisplay;
    @Schema(description = "End date in ISO 8601 format")
    public String endDisplayISO8601;
    @Schema(description = "Cancellation date timestamp")
    public Long canceledDate;
    @Schema(description = "Cancellation date value")
    public Long canceledDateValue;
    @Schema(description = "Cancellation date in seconds since epoch")
    public Long canceledDateInSeconds;
    @Schema(description = "Formatted cancellation date")
    public String canceledDateDisplay;
    @Schema(description = "Cancellation date in ISO 8601 format")
    public String canceledDateDisplayISO8601;
    @Schema(description = "Deactivation date timestamp")
    public Long deactivationDate;
    @Schema(description = "Deactivation date value")
    public Long deactivationDateValue;
    @Schema(description = "Deactivation date in seconds since epoch")
    public Long deactivationDateInSeconds;
    @Schema(description = "Formatted deactivation date")
    public String deactivationDateDisplay;
    @Schema(description = "Deactivation date in ISO 8601 format")
    public String deactivationDateDisplayISO8601;
    @Schema(description = "Billing sequence number")
    public int sequence;
    @Schema(description = "Total number of billing periods")
    public Long periods;
    @Schema(description = "Remaining billing periods")
    public Long remainingPeriods;
    @Schema(description = "Subscription begin date timestamp")
    public long begin;
    @Schema(description = "Begin date value")
    public long beginValue;
    @Schema(description = "Begin date in seconds since epoch")
    public int beginInSeconds;
    @Schema(description = "Formatted begin date")
    public String beginDisplay;
    @Schema(description = "Begin date in ISO 8601 format")
    public String beginDisplayISO8601;
    @Schema(description = "Billing interval unit (e.g., month, year)")
    public String intervalUnit;
    @Schema(description = "Length of the billing interval")
    public int intervalLength;
    @Schema(description = "Currency for the next charge")
    public String nextChargeCurrency;
    @Schema(description = "Next charge date timestamp")
    public long nextChargeDate;
    @Schema(description = "Next charge date value")
    public long nextChargeDateValue;
    @Schema(description = "Next charge date in seconds since epoch")
    public int nextChargeDateInSeconds;
    @Schema(description = "Formatted next charge date")
    public String nextChargeDateDisplay;
    @Schema(description = "Next charge date in ISO 8601 format")
    public String nextChargeDateDisplayISO8601;
    @Schema(description = "Next charge pre-tax amount")
    public int nextChargePreTax;
    @Schema(description = "Formatted next charge pre-tax")
    public String nextChargePreTaxDisplay;
    @Schema(description = "Next charge pre-tax in payout currency")
    public int nextChargePreTaxInPayoutCurrency;
    @Schema(description = "Formatted next charge pre-tax in payout currency")
    public String nextChargePreTaxInPayoutCurrencyDisplay;
    @Schema(description = "Total next charge amount including tax")
    public int nextChargeTotal;
    @Schema(description = "Formatted next charge total")
    public String nextChargeTotalDisplay;
    @Schema(description = "Next charge total in payout currency")
    public int nextChargeTotalInPayoutCurrency;
    @Schema(description = "Formatted next charge total in payout currency")
    public String nextChargeTotalInPayoutCurrencyDisplay;
    @Schema(description = "Type of the next notification to send")
    public String nextNotificationType;
    @Schema(description = "Next notification date timestamp")
    public long nextNotificationDate;
    @Schema(description = "Next notification date value")
    public long nextNotificationDateValue;
    @Schema(description = "Next notification date in seconds since epoch")
    public int nextNotificationDateInSeconds;
    @Schema(description = "Formatted next notification date")
    public String nextNotificationDateDisplay;
    @Schema(description = "Next notification date in ISO 8601 format")
    public String nextNotificationDateDisplayISO8601;
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
    @Schema(description = "Initial order ID that created this subscription")
    public String initialOrderId;
    @Schema(description = "Initial order reference for this subscription")
    public String initialOrderReference;
}
