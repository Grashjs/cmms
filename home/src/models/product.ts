export interface Product {
  id: string;
  name: string;
  price: number;
  sale_price: number;
  stock: number;
  rating: number;
  images: string[];
  orders: number;
  summary: string;
  description: string;
  categories: string[];
}
