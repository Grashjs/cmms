import { Box, Button, Container, Grid, Stack, Typography } from "@mui/material";
import React from "react";
import { getLocale, getTranslations } from "next-intl/server";
import { getBrandServer as getBrandConfig } from "src/utils/serverBrand";
import { BoxAccent, MobileImgWrapper, TypographyH2 } from "./styles";
import MainAppLink from "src/components/MainAppLink";
import { getWorkOrdersUrl } from "src/utils/urlPaths";
import Image from "next/image";
import SignupButtonServer from "src/components/SignupButtonServer";

async function Hero() {
  const t = await getTranslations();
  const locale = await getLocale();
  const brandConfig = await getBrandConfig();

  return (
    <Container maxWidth="lg">
      <Grid spacing={{ xs: 6, md: 10 }} justifyContent="center" alignItems="center" container>
        <Grid item md={6} pr={{ xs: 0, md: 4 }}>
          <Typography component="h1" variant="h4" mb={2}>
            {t("home.h1")}
          </Typography>
          <Typography
            sx={{
              mb: 2,
            }}
            fontSize={50}
            variant="h1"
            component="h2"
          >
            {t("home.h2")}
          </Typography>
          <TypographyH2
            sx={{
              lineHeight: 1.5,
              pb: 4,
            }}
            variant="h4"
            color="text.secondary"
            fontWeight="normal"
          >
            {t("home.h3", { brandName: brandConfig.name })}
          </TypographyH2>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1}>
            <SignupButtonServer size="large" variant="contained">
              {t("try_for_free")}
            </SignupButtonServer>
            {/*<LiveDemoButton />*/}
            <Button
              sx={{
                ml: 2,
              }}
              href={`mailto:${brandConfig.mail}`}
              size="medium"
              variant="text"
            >
              {t("talk_to_sales")}
            </Button>
          </Stack>
        </Grid>
        <Grid item md={6}>
          <Box
            sx={{
              width: "150%",
              position: "relative",
            }}
          >
            <MainAppLink href={getWorkOrdersUrl(locale)}>
              <Box
                sx={{
                  position: "relative",
                  zIndex: 5,
                  width: "100%",
                  overflow: "hidden",
                  borderRadius: "12px",
                  boxShadow:
                    "0 0rem 14rem 0 rgb(255 255 255 / 20%), 0 0.8rem 2.3rem rgb(111 130 156 / 3%), 0 0.2rem 0.7rem rgb(17 29 57 / 15%)",
                  aspectRatio: "1920 / 922",
                  "& img": {
                    display: "block",
                    width: "100%",
                  },
                }}
              >
                <Image
                  alt={brandConfig.name}
                  src="/static/images/overview/work_orders_screenshot.png"
                  width={1920}
                  height={922}
                  preload
                />
              </Box>
            </MainAppLink>
            <MobileImgWrapper>
              <Image alt="Mobile App" src="/static/mobile_app.jpeg" width={720} height={1600} loading={"eager"} />
            </MobileImgWrapper>
            <BoxAccent
              sx={{
                display: { xs: "none", md: "block" },
              }}
            />
          </Box>
        </Grid>
      </Grid>
    </Container>
  );
}

export default Hero;
