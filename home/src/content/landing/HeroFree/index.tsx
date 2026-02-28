import { Button, Container, Grid, Stack, Typography } from "@mui/material";
import { getLocale, getTranslations } from "next-intl/server";
import { getSignupUrl } from "src/utils/urlPaths";
import { BoxAccent, BoxContent, MobileImgWrapper, TypographyH2 } from "../styled";
import { ImgWrapper } from "src/content/overview/Hero/styles";
import Image from "next/image";

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
            <Button
              size="large"
              variant="contained"
              href={getSignupUrl(locale)}
              target="_blank"
              rel="noopener noreferrer"
            >
              {t("free_cmms.hero.start_free")}
            </Button>
          </Stack>
        </Grid>
        <Grid item md={6}>
          <BoxContent>
            <a href={getSignupUrl(locale)}>
              <ImgWrapper>
                <Image
                  alt={t("free_cmms.hero.work_orders_alt")}
                  src="/static/images/overview/work_orders_screenshot.png"
                  width={1920}
                  height={922}
                />
              </ImgWrapper>
            </a>
            <MobileImgWrapper>
              <Image height={1600} width={720} alt={t("free_cmms.hero.mobile_app_alt")} src="/static/mobile_app.jpeg" />
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

export default HeroFree;
