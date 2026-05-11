import { Box, Container, Grid, Stack, Typography } from "@mui/material";
import { getLocale, getTranslations } from "next-intl/server";
import MainAppLink from "src/components/MainAppLink";
import { BoxAccent, BoxContent, MobileImgWrapper, TypographyH2 } from "../styled";
import Image from "next/image";
import { getSignupUrl } from "src/utils/urlPaths";
import SignupButtonServer from "src/components/SignupButtonServer";

async function HeroFree() {
  const t = await getTranslations();
  const locale = await getLocale();

  return (
    <Container maxWidth="lg">
      <Grid spacing={{ xs: 6, md: 10 }} justifyContent="center" alignItems="center" container>
        <Grid item md={6} pr={{ xs: 0, md: 4 }}>
          <Typography component="h1" variant="h4" mb={2}>
            {t("free_cmms.hero.subtitle")}
          </Typography>
          <Typography
            sx={{
              mb: 2,
            }}
            fontSize={45}
            variant="h2"
            component="h2"
          >
            {t("free_cmms.hero.title")}
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
            {t("free_cmms.hero.description")}
          </TypographyH2>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1}>
            <SignupButtonServer size="large" variant="contained">
              {t("free_cmms.hero.start_free")}
            </SignupButtonServer>
          </Stack>
        </Grid>
        <Grid item md={6}>
          <Box
            sx={{
              width: "150%",
              position: "relative",
            }}
          >
            <MainAppLink href={getSignupUrl(locale)}>
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
                  alt={t("free_cmms.hero.work_orders_alt")}
                  src="/static/images/overview/work_orders_screenshot.png"
                  width={1920}
                  height={922}
                />
              </Box>
            </MainAppLink>
            <MobileImgWrapper>
              <Image height={1600} width={720} alt={t("free_cmms.hero.mobile_app_alt")} src="/static/mobile_app.jpeg" />
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

export default HeroFree;
