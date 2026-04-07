import { Audit } from './audit';
import { Vendor } from './vendor';
import { PartQuantityMiniDTO } from './partQuantity';
import Category from './category';

export type PurchaseOrderStatus = 'APPROVED' | 'PENDING' | 'REJECTED';
export default interface PurchaseOrder extends Audit {
  status: PurchaseOrderStatus;
  name: string;
  id: number;
  category: Category;
  additionalDetails: string;
  vendor?: Vendor;
  shippingDueDate: string;
  shippingAdditionalDetail: string;
  shippingShipToName: string;
  shippingCompanyName: string;
  shippingAddress: string;
  shippingCity: string;
  shippingState: string;
  shippingZipCode: string;
  shippingPhone: string;
  shippingFax: string;
  additionalInfoRequisitionedName: string;
  additionalInfoShippingMethod: string;
  additionalInfoShippingOrderCategory: string;
  additionalInfoTerm: string;
  additionalInfoNotes: string;
  partQuantities: PartQuantityMiniDTO[];
}
