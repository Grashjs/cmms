"use client";

import { Box, styled } from "@mui/material";
import { useRouter } from "src/i18n/routing";
import { useTranslations } from "next-intl";
import Hero from "./Hero";
import Highlights from "./Highlights";
import NavBar from "src/components/NavBar";
import { useBrand } from "src/hooks/useBrand";
import { Footer } from "src/components/Footer";
import CompanyLogos from "src/components/CompanyLogos";

const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`,
);
function Overview() {
  const t = useTranslations();
  const router = useRouter();
  const brandConfig = useBrand();

  return (
    <OverviewWrapper>
      <NavBar />
      <Hero />
      <CompanyLogos sx={{ mt: { xs: "150px", md: "100px" } }} />
      <Highlights />
      <Footer />
    </OverviewWrapper>
  );
}

export default Overview;
