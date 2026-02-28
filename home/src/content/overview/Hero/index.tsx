import { Button, Container, Grid, Stack, Typography } from "@mui/material";
import React from "react";
import { getLocale, getTranslations } from "next-intl/server";
import { Link } from "src/i18n/routing";
import { getBrandServer as getBrandConfig } from "src/utils/serverBrand";
import LiveDemoButton from "./LiveDemoButton";
import { BoxAccent, BoxContent, ImgWrapper, MobileImgWrapper, TypographyH2 } from "./styles";
import { getSignupUrl, getWorkOrdersUrl } from "src/utils/urlPaths";

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
            <Link href={getSignupUrl(locale)}>
              <Button size="large" variant="contained">
                {t("try_for_free")}
              </Button>
            </Link>
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
          <BoxContent>
            <Link href={getWorkOrdersUrl(locale)}>
              <ImgWrapper>
                <img
                  alt={brandConfig.name}
                  src="/static/images/overview/work_orders_screenshot.png"
                  loading={"eager"}
                />
              </ImgWrapper>
            </Link>
            <MobileImgWrapper>
              <img alt="Mobile App" src="/static/mobile_app.jpeg" loading={"eager"} />
            </MobileImgWrapper>
            <BoxAccent
              sx={{
                display: { xs: "none", md: "block" },
              }}
            />
          </BoxContent>
        </Grid>
      </Grid>
    </Container>
  );
}

export default Hero;
