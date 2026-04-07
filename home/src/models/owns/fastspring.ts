export interface Payment {
  type: string;
  cardEnding: string;
}

export interface Item {
  product: string;
  quantity: number;
  coupon?: any;
  sku?: any;
  subtotal: number;
  subtotalDisplay: string;
  subtotalInPayoutCurrency: number;
  subtotalInPayoutCurrencyDisplay: string;
  discount: number;
  discountDisplay: string;
  discountInPayoutCurrency: number;
  discountInPayoutCurrencyDisplay: string;
  subscription: string;
}

export interface Order {
  id: string | null;
  reference: string;
  buyerReference?: any;
  live: boolean;
  currency: string;
  payoutCurrency: string;
  total: number;
  totalDisplay: string;
  totalInPayoutCurrency: number;
  totalInPayoutCurrencyDisplay: string;
  tax: number;
  taxDisplay: string;
  taxInPayoutCurrency: number;
  taxInPayoutCurrencyDisplay: string;
  subtotal: number;
  subtotalDisplay: string;
  subtotalInPayoutCurrency: number;
  subtotalInPayoutCurrencyDisplay: string;
  discount: number;
  discountDisplay: string;
  discountInPayoutCurrency: number;
  discountInPayoutCurrencyDisplay: string;
  discountWithTax: number;
  discountWithTaxDisplay: string;
  discountWithTaxInPayoutCurrency: number;
  discountWithTaxInPayoutCurrencyDisplay: string;
  payment: Payment;
  account: string;
  items: Item[];
}
