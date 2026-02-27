"use client";
import { useTranslations } from "next-intl";
import { SetStateAction, useState } from "react";
import { Box, Tab, Tabs, useTheme, Link } from "@mui/material";
import SubscriptionPlanSelector from "src/content/pricing/components/SubscriptionPlanSelector";
import TwoCallToActions from "src/content/landing/components/TwoCallToActions";
import { TypographyH1Primary } from "./styles";

export default function PricingSection({ hidePricing }: { hidePricing?: boolean }) {
  const t = useTranslations();
  const theme = useTheme();
  const [monthly, setMonthly] = useState<boolean>(true);
  const [pricingType, setPricingType] = useState<"cloud" | "selfhosted">("cloud");

  const handlePricingTabChange = (_event: any, value: SetStateAction<"cloud" | "selfhosted">) => {
    setPricingType(value);
  };

  return (
    <>
      {!hidePricing && (
        <>
          <TypographyH1Primary
            textAlign="center"
            sx={{
              mt: 8,
              mb: 2,
            }}
            variant="h2"
          >
            {t("choose_your_plan")}
          </TypographyH1Primary>
          <Box sx={{ display: "flex", justifyContent: "center", mb: 2 }}>
            <Tabs value={pricingType} onChange={handlePricingTabChange} indicatorColor="primary" textColor="primary">
              <Tab label={t("cloud")} value="cloud" />
              <Tab label={t("self_hosted")} value="selfhosted" />
            </Tabs>
          </Box>
          {pricingType === "selfhosted" && (
            <Box sx={{ display: "flex", justifyContent: "center", mb: 1 }}>
              <Link
                style={{ color: theme.palette.primary.main }}
                href={"https://github.com/Grashjs/cmms?tab=readme-ov-file#self-host--run-locally"}
              >
                {t("installation_docs")}
              </Link>
            </Box>
          )}
          <Box px={4}>
            <SubscriptionPlanSelector
              monthly={monthly}
              setMonthly={setMonthly}
              selfHosted={pricingType === "selfhosted"}
            />
          </Box>
        </>
      )}
      <TwoCallToActions
        hidePricing={hidePricing}
        sx={{
          pt: { xs: 6, md: 12 },
          pb: { xs: 5, md: 15 },
        }}
      />
    </>
  );
}
