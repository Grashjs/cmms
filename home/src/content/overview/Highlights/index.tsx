import { getTranslations } from "next-intl/server";
import { Box, Container, Grid, Typography } from "@mui/material";
import { getBrandServer as getBrandConfig } from "src/utils/serverBrand";
import FeatureTabs from "./FeatureTabs";
import PricingSection from "./PricingSection";
import {
  BoxHighlights,
  BoxLayouts,
  BoxLayoutsContent,
  BoxLayoutsImage,
  BoxWave,
  BoxWaveAlt,
  LayoutImgButton,
  TypographyFeature,
  TypographyHeading,
  TypographySubHeading,
} from "./styles";
import Image from "next/image";

async function Highlights({ hidePricing }: { hidePricing?: boolean }) {
  const t = await getTranslations();
  const brandConfig = await getBrandConfig();

  return (
    <BoxHighlights>
      <BoxLayouts>
        <BoxWave>
          <svg viewBox="0 0 1440 172" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M0 0H1440V52.1874C1440 52.1874 873.5 172 720 172C566.5 172 0 52.1874 0 52.1874V0Z" fill="white" />
          </svg>
        </BoxWave>
        <BoxLayoutsImage
          sx={{
            backgroundImage: 'url("/static/images/placeholders/covers/7.jpg")',
          }}
        />
        <BoxLayoutsContent maxWidth="lg">
          <Grid justifyContent="center" alignItems="center" spacing={6} container>
            <Grid item xs={12} md={6}>
              <TypographyFeature
                className="typo-feature"
                sx={{
                  mt: { lg: 10 },
                }}
              >
                {t("home.what")}
              </TypographyFeature>
              <TypographyHeading
                className="typo-heading"
                sx={{
                  mb: 1,
                }}
                variant="h3"
              >
                {t("home.you_will_have")}
              </TypographyHeading>
              <TypographySubHeading
                className="typo-subheading"
                sx={{
                  lineHeight: 1.5,
                }}
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
              >
                {t("home.you_will_have_description")}
              </TypographySubHeading>
            </Grid>
            <Grid item xs={12} md={6}>
              <LayoutImgButton>
                <Typography variant="h4">{t("work_orders")}</Typography>
                <Image src="/static/images/overview/work_order_screenshot.png" alt={t("work_orders")} width={1920} height={922} />
              </LayoutImgButton>
            </Grid>
            <Grid item xs={12} md={6}>
              <LayoutImgButton>
                <Typography variant="h4">{t("custom_dashboards")}</Typography>
                <Image src="/static/images/overview/analytics_screenshot.png" alt={t("custom_dashboards")} width={1920} height={922} />
              </LayoutImgButton>
            </Grid>
            <Grid item xs={12} md={6}>
              <LayoutImgButton>
                <Typography variant="h4">{t("asset_management")}</Typography>
                <Image src="/static/images/overview/assets.png" alt={t("asset_management")} width={1920} height={869} />
              </LayoutImgButton>
            </Grid>
          </Grid>
        </BoxLayoutsContent>
        <BoxWaveAlt>
          <svg viewBox="0 0 1440 172" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M1440 172H0V119.813C0 119.813 566.5 0 720 0C873.5 0 1440 119.813 1440 119.813V172Z" fill="white" />
          </svg>
        </BoxWaveAlt>
      </BoxLayouts>
      <Container
        maxWidth="lg"
        sx={{
          py: { xs: 8, md: 10 },
        }}
      >
        <Grid spacing={0} direction={{ xs: "column-reverse", md: "row" }} justifyContent="center" container>
          <Grid item xs={12} md={6}>
            <Box>
              <TypographyHeading
                sx={{
                  mb: 1,
                }}
                variant="h3"
              >
                {t("home.work")}
              </TypographyHeading>
              <TypographyFeature>{t("home.smarter")}</TypographyFeature>
              <TypographySubHeading
                sx={{
                  lineHeight: 1.5,
                  pr: 8,
                }}
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
              >
                {t("home.smarter_description", {
                  shortBrandName: brandConfig.name,
                })}
              </TypographySubHeading>
            </Box>
          </Grid>
        </Grid>
        <FeatureTabs brandConfig={brandConfig} />
      </Container>
      <PricingSection hidePricing={hidePricing} />
    </BoxHighlights>
  );
}

export default Highlights;
