// BrandContext.tsx
"use client";
import { createContext, useContext } from "react";
import { BrandConfig } from "src/utils/serverBrand";

const BrandContext = createContext<BrandConfig | null>(null);

export const BrandProvider = ({ brand, children }: { brand: BrandConfig; children: React.ReactNode }) => (
  <BrandContext.Provider value={brand}>{children}</BrandContext.Provider>
);

export const useBrand = () => {
  const ctx = useContext(BrandContext);
  if (!ctx) throw new Error("useBrand must be used within BrandProvider");
  return ctx;
};
