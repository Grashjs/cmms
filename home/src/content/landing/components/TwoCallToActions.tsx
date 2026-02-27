"use client";
import { Button, Container, Stack, SxProps, Theme } from "@mui/material";
import { TypographyH1Primary } from "src/content/overview/Highlights/styles";
import { useTranslations } from "next-intl";
import { Link } from "src/i18n/routing";
import { demoLink } from "src/config";
import { signupUrl } from "src/utils/urlPaths";

export default function TwoCallToActions({ hidePricing, sx }: { hidePricing?: boolean; sx?: SxProps<Theme> }) {
  const t = useTranslations();
  return (
    <Container sx={sx} maxWidth="md">
      <TypographyH1Primary
        textAlign="center"
        sx={{
          mb: 2,
        }}
        variant="h2"
      >
        {t("cut_costs_performance")}
      </TypographyH1Primary>
      <Container
        sx={{
          mb: 6,
          justifyContent: "center",
        }}
        maxWidth="sm"
      >
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent={"center"} spacing={2}>
          <Button component={Link} size="large" href={signupUrl} variant="contained">
            {hidePricing ? "Sign Up for Free" : t("register")}
          </Button>
          {!hidePricing && (
            <Button size="large" href={demoLink} variant="outlined">
              {t("book_demo")}
            </Button>
          )}
        </Stack>
      </Container>
    </Container>
  );
}
