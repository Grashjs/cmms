export type InvoiceStatus = 'pending' | 'completed' | 'draft' | 'progress';

export interface Invoice {
  id: string;
  issuedDate: number;
  number: string;
  dueDate: number;
  clientName: string;
  clientAvatar: string;
  amount: number;
  currency: string;
  status: InvoiceStatus;
  [key: string]: any;
}
