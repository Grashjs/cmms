import React, { FC, ReactNode } from "react";
import { Box, Button, Card, CardContent, Container, Grid, Stack, Typography } from "@mui/material";

import { StyledAdvantageCard, StyledAdvantageIconWrapper, StyledFeatureNumber, StyledKpiValue } from "./styles";
import NavBar from "src/components/NavBar";
import Footer from "src/components/Footer";
import FaqComponent from "src/components/Faq";
import { demoLink } from "src/config";
import { OverviewWrapper } from "src/content/landing/FreeCMMS";
import { TypographyH2 } from "src/content/landing/HeroFree";
import { SvgIconComponent } from "@mui/icons-material";
import TwoCallToActions from "../../content/landing/components/TwoCallToActions";
import Testimonials, { Testimonial } from "../../content/landing/components/Testimonials";
import { Link } from "src/i18n/routing";
import CompanyLogos from "@/src/components/CompanyLogos";

import { getTranslations } from "next-intl/server";
import { signupUrl } from "@/src/utils/urlPaths";

interface Feature {
  title: string;
  description: string;
  imageUrl: string;
  learnMoreUrl?: string;
}

interface FAQ {
  question: string;
  answer: string;
}

interface RelatedContent {
  title: string;
  imageUrl: string;
  url: string;
}

export interface IndustryLayoutProps {
  pageTitle: string;
  headerTitle: string;
  headerSubtitle: string;
  headerImageUrl: string;
  headerImageSizes: { width: number; height: number };
  companyLogos?: boolean;
  advantages?: { title: string; description: string; icon: SvgIconComponent }[];
  kpis?: { title: string; value: string; type: "money" | "percentage" }[];
  features?: Feature[];
  testimonials: Testimonial[];
  faqs: FAQ[];
  relatedContent: RelatedContent[];
  children?: ReactNode;
  pageDescription: string;
  canonicalPath: string;
}

const IndustryLayout: FC<IndustryLayoutProps> = async (props) => {
  const {
    pageTitle,
    headerTitle,
    headerSubtitle,
    headerImageUrl,
    headerImageSizes,
    companyLogos,
    features = [],
    advantages = [],
    testimonials,
    faqs,
    relatedContent,
    kpis,
    children,
    pageDescription,
    canonicalPath,
  } = props;
  const t = await getTranslations();

  return (
    <OverviewWrapper>
      <NavBar />

      <Box>
        {/* Header */}
        <Container maxWidth="lg">
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography component="h1" variant="h4" mb={2}>
                {pageTitle}
              </Typography>
              <Typography fontSize={45} variant="h2" component="h2" gutterBottom>
                {headerTitle}
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
                {headerSubtitle}
              </TypographyH2>
              <Stack direction="row" spacing={2}>
                <Link href={signupUrl}>
                  <Button size={"large"} variant={"contained"}>
                    {t("try_for_free")}
                  </Button>
                </Link>
                <Button size="large" href={demoLink} variant="outlined">
                  {t("book_demo")}
                </Button>
              </Stack>
            </Grid>
            <Grid item xs={12} md={6}>
              <img
                src={headerImageUrl}
                alt={headerTitle}
                width={headerImageSizes.width}
                height={headerImageSizes.height}
                style={{ width: "100%", borderRadius: "16px", height: "auto" }}
                loading="eager"
                fetchPriority="high"
              />
            </Grid>
          </Grid>
        </Container>

        {/* Company Logos */}
        {kpis && (
          <Container maxWidth="lg" sx={{ py: 5 }}>
            <Grid container spacing={3}>
              {kpis.map((kpi) => (
                <Grid item xs={12} sm={6} md={4} key={kpi.title}>
                  <Card sx={{ p: 3, height: "100%" }}>
                    <CardContent
                      sx={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                      }}
                    >
                      <StyledKpiValue>
                        <span className="money-icon">{kpi.type === "money" ? "$" : ""}</span>
                        {kpi.value}
                        <span className="percentage-icon">{kpi.type === "percentage" ? "%" : ""}</span>
                      </StyledKpiValue>
                      <Typography textAlign={"center"} fontWeight={600} gutterBottom>
                        {kpi.title}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Container>
        )}
        {companyLogos && <CompanyLogos />}
        {advantages.length > 0 && (
          <Container maxWidth={"lg"} sx={{ mt: 2, py: 5 }}>
            <Grid container spacing={4}>
              {advantages.map((advantage, index) => {
                const Icon = advantage.icon;
                return (
                  <Grid item xs={12} sm={6} md={4} key={index}>
                    <StyledAdvantageCard>
                      <StyledAdvantageIconWrapper>
                        <Icon sx={{ fontSize: 32 }} />
                      </StyledAdvantageIconWrapper>
                      <CardContent sx={{ p: 0 }}>
                        <Typography variant="h3" gutterBottom sx={{ mb: 2 }}>
                          {advantage.title}
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                          {advantage.description}
                        </Typography>
                      </CardContent>
                    </StyledAdvantageCard>
                  </Grid>
                );
              })}
            </Grid>
          </Container>
        )}

        {/* Features */}
        <Container maxWidth="lg" sx={{ py: 8 }}>
          {features.map((feature, index) => (
            <Grid container spacing={4} key={index} alignItems="center" sx={{ mb: 4 }}>
              <Grid item xs={12} md={6} order={{ xs: 2, md: index % 2 === 0 ? 1 : 2 }}>
                <Typography variant="h3" gutterBottom>
                  {feature.title}
                </Typography>
                <Typography color={"text.primary"} variant="body1" paragraph>
                  {feature.description}
                </Typography>
                {feature.learnMoreUrl ? (
                  <Link href={feature.learnMoreUrl}>
                    <Button variant="outlined">Learn More</Button>
                  </Link>
                ) : (
                  <Link href={"/account/register"}>
                    <Button variant={"outlined"}>{t("try_for_free")}</Button>
                  </Link>
                )}
              </Grid>
              <Grid item xs={12} md={6} order={{ xs: 1, md: index % 2 === 0 ? 2 : 1 }}>
                {/*<img*/}
                {/*  src={feature.imageUrl}*/}
                {/*  alt={feature.title}*/}
                {/*  style={{ width: '100%' }}*/}
                {/*/>*/}
                <StyledFeatureNumber>{(index + 1).toString().padStart(2, "0")}</StyledFeatureNumber>
              </Grid>
            </Grid>
          ))}
          {children}
        </Container>

        {/* Testimonials */}
        {testimonials?.length > 0 && <Testimonials testimonials={testimonials} />}
        <TwoCallToActions />
        {/* FAQ */}
        <Container maxWidth="lg">
          <FaqComponent
            title="FAQ"
            items={faqs.map((faq) => ({
              title: faq.question,
              content: <Typography variant="body1">{faq.answer}</Typography>,
            }))}
          />
        </Container>

        {/* Related Content */}
        {relatedContent.length > 0 && (
          <Container maxWidth="lg" sx={{ mb: 8 }}>
            <Typography variant="h2" align="center" mb={3}>
              Related Content
            </Typography>
            <Grid container spacing={4}>
              {relatedContent.map((content, index) => (
                <Grid item xs={12} md={4} key={index}>
                  <Card sx={{ height: "100%" }}>
                    <img src={content.imageUrl} alt={content.title} style={{ width: "100%" }} />
                    <CardContent>
                      <Typography variant="h6">{content.title}</Typography>
                      <Button href={content.url}>Read More</Button>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Container>
        )}
      </Box>
      <Footer />
    </OverviewWrapper>
  );
};

export default IndustryLayout;
