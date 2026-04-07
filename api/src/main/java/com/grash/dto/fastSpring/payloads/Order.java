package com.grash.dto.fastSpring.payloads;

import com.grash.dto.fastSpring.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;

@Data
@Schema(description = "FastSpring order details payload")
public class Order {
    @Schema(description = "Order reference number")
    public String order;
    @Schema(description = "Order identifier")
    public String id;
    @Schema(description = "Order reference")
    public String reference;
    @Schema(description = "Buyer reference or identifier")
    public String buyerReference;
    @Schema(description = "Customer IP address")
    public String ipAddress;
    @Schema(description = "Whether the order has been completed")
    public boolean completed;
    @Schema(description = "Timestamp when the order was last changed")
    public long changed;
    @Schema(description = "Changed date value")
    public long changedValue;
    @Schema(description = "Changed date in seconds since epoch")
    public int changedInSeconds;
    @Schema(description = "Formatted changed date display")
    public String changedDisplay;
    @Schema(description = "Changed date in ISO 8601 format")
    public String changedDisplayISO8601;
    @Schema(description = "Order language preference")
    public String language;
    @Schema(description = "Whether this is a live order (vs sandbox)")
    public boolean live;
    @Schema(description = "Order currency code")
    public String currency;
    @Schema(description = "Payout currency code")
    public String payoutCurrency;
    @Schema(description = "Quote reference for the order")
    public String quote;
    @Schema(description = "URL to the order invoice")
    public String invoiceUrl;
    @Schema(description = "Account associated with this order")
    public Account account;
    @Schema(description = "Total order amount")
    public int total;
    @Schema(description = "Formatted total amount")
    public String totalDisplay;
    @Schema(description = "Total in payout currency")
    public int totalInPayoutCurrency;
    @Schema(description = "Formatted total in payout currency")
    public String totalInPayoutCurrencyDisplay;
    @Schema(description = "Tax amount")
    public int tax;
    @Schema(description = "Formatted tax amount")
    public String taxDisplay;
    @Schema(description = "Tax in payout currency")
    public int taxInPayoutCurrency;
    @Schema(description = "Formatted tax in payout currency")
    public String taxInPayoutCurrencyDisplay;
    @Schema(description = "Subtotal amount before tax and discounts")
    public int subtotal;
    @Schema(description = "Formatted subtotal amount")
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
    @Schema(description = "Discount amount including tax")
    public int discountWithTax;
    @Schema(description = "Formatted discount with tax")
    public String discountWithTaxDisplay;
    @Schema(description = "Discount with tax in payout currency")
    public int discountWithTaxInPayoutCurrency;
    @Schema(description = "Formatted discount with tax in payout currency")
    public String discountWithTaxInPayoutCurrencyDisplay;
    @Schema(description = "Billing descriptor shown on customer statement")
    public String billDescriptor;
    @Schema(description = "Payment method details")
    public Payment payment;
    @Schema(description = "Customer information for this order")
    public Customer customer;
    @Schema(description = "Customer address for this order")
    public Address address;
    @Schema(description = "List of gift recipients for this order")
    public ArrayList<Recipient> recipients;
    @Schema(description = "Tags associated with this order")
    public Tags tags;
    @Schema(description = "Order notes")
    public ArrayList<Object> notes;
    @Schema(description = "Line items in this order")
    public ArrayList<Item> items;
}
