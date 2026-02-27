"use client";
import React, { FC, ReactNode } from 'react';
import {
  Box,
  Container,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  Stack,
  useTheme
} from '@mui/material';
import NavBar from 'src/components/NavBar';
import { Footer } from 'src/components/Footer';
import FaqComponent from 'src/components/Faq';
import { Helmet } from 'react-helmet-async';
import Link from 'next/link';
import { useTranslation } from 'react-i18next';
import { demoLink } from 'src/config';
import { OverviewWrapper } from 'src/content/landing/FreeCMMS';
import { TypographyH2 } from 'src/content/landing/HeroFree';
import { SvgIconComponent } from '@mui/icons-material';
import TwoCallToActions from 'src/content/landing/components/TwoCallToActions';

interface Feature {
  title: string;
  description: string;
  imageUrl: string;
  learnMoreUrl?: string;
}

interface Testimonial {
  text: string;
  author: string;
  company: string;
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
  companyLogos?: string[];
  advantages?: { title: string; description: string; icon: SvgIconComponent }[];
  kpis?: { title: string; value: string; type: 'money' | 'percentage' }[];
  features?: Feature[];
  testimonials: Testimonial[];
  faqs: FAQ[];
  relatedContent: RelatedContent[];
  children?: ReactNode;
  pageDescription: string;
  canonicalPath: string;
}

const IndustryLayout: FC<IndustryLayoutProps> = (props) => {
  const {
    pageTitle,
    headerTitle,
    headerSubtitle,
    headerImageUrl,
    companyLogos,
    features = [],
    advantages = [],
    testimonials,
    faqs,
    relatedContent,
    kpis,
    children,
    pageDescription,
    canonicalPath
  } = props;
  const { t } = useTranslation();
  const theme = useTheme();

  return (
    <OverviewWrapper>
      <Helmet>
        <meta name="description" content={pageDescription} />
        <title>{pageTitle}</title>
        <link
          rel="canonical"
          href={'https://atlas-cmms.com/' + canonicalPath}
        />
      </Helmet>
      <NavBar />
      <Box>
        {/* Header */}
        <Container maxWidth="lg">
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography component="h1" variant="h4" mb={2}>
                {pageTitle}
              </Typography>
              <Typography
                fontSize={45}
                variant="h2"
                component="h2"
                gutterBottom
              >
                {headerTitle}
              </Typography>
              <TypographyH2
                sx={{
                  lineHeight: 1.5,
                  pb: 4
                }}
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
              >
                {headerSubtitle}
              </TypographyH2>
              <Stack direction="row" spacing={2}>
                <Button
                  variant={'contained'}
                  component={Link}
                  href="/account/register"
                >
                  {t('try_for_free')}
                </Button>
                <Button size="large" href={demoLink} variant="outlined">
                  {t('book_demo')}
                </Button>
              </Stack>
            </Grid>
            <Grid item xs={12} md={6}>
              <Box
                component="img"
                src={headerImageUrl}
                alt={headerTitle}
                sx={{ width: '100%', borderRadius: '16px' }}
              />
            </Grid>
          </Grid>
        </Container>

        {/* Company Logos */}
        {false && companyLogos && (
          <Container maxWidth="lg" sx={{ py: 5 }}>
            <Grid container spacing={4} justifyContent="center">
              {companyLogos.map((logo, index) => (
                <Grid item key={index}>
                  <img
                    style={{
                      filter: 'grayscale(100%)'
                    }}
                    src={logo}
                    alt={`company-logo-${index}`}
                    height="30"
                  />
                </Grid>
              ))}
            </Grid>
          </Container>
        )}
        {kpis && (
          <Container maxWidth="lg" sx={{ py: 5 }}>
            <Grid container spacing={3}>
              {kpis.map((kpi) => (
                <Grid item xs={12} sm={6} md={4} key={kpi.title}>
                  <Card sx={{ p: 3, height: '100%' }}>
                    <CardContent
                      sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center'
                      }}
                    >
                      <Typography fontSize={50} fontWeight={600}>
                        <span style={{ color: theme.palette.primary.main }}>
                          {kpi.type === 'money' ? '$' : ''}
                        </span>
                        {kpi.value}
                        <span style={{ color: theme.palette.primary.main }}>
                          {kpi.type === 'percentage' ? '%' : ''}
                        </span>
                      </Typography>
                      <Typography
                        textAlign={'center'}
                        fontWeight={600}
                        gutterBottom
                      >
                        {kpi.title}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Container>
        )}
        {advantages.length > 0 && (
          <Container maxWidth={'lg'} sx={{ mt: 2, py: 5 }}>
            <Grid container spacing={4}>
              {advantages.map((advantage, index) => {
                const Icon = advantage.icon;
                return (
                  <Grid item xs={12} sm={6} md={4} key={index}>
                    <Card
                      sx={{
                        height: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        textAlign: 'center',
                        p: 3,
                        transition: 'transform 0.2s',
                        '&:hover': {
                          transform: 'translateY(-5px)',
                          boxShadow: theme.shadows[4]
                        }
                      }}
                    >
                      <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          width: 64,
                          height: 64,
                          borderRadius: '16px',
                          backgroundColor: theme.palette.primary.main,
                          color: theme.palette.primary.contrastText,
                          mb: 3,
                          boxShadow: `0 4px 20px 0 ${theme.palette.primary.main}40`
                        }}
                      >
                        <Icon sx={{ fontSize: 32 }} />
                      </Box>
                      <CardContent sx={{ p: 0 }}>
                        <Typography variant="h3" gutterBottom sx={{ mb: 2 }}>
                          {advantage.title}
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                          {advantage.description}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                );
              })}
            </Grid>
          </Container>
        )}

        {/* Features */}
        <Container maxWidth="lg" sx={{ py: 8 }}>
          {features.map((feature, index) => (
            <Grid
              container
              spacing={4}
              key={index}
              alignItems="center"
              sx={{ mb: 4 }}
            >
              <Grid
                item
                xs={12}
                md={6}
                order={{ xs: 2, md: index % 2 === 0 ? 1 : 2 }}
              >
                <Typography variant="h3" gutterBottom>
                  {feature.title}
                </Typography>
                <Typography variant="body1" paragraph>
                  {feature.description}
                </Typography>
                {feature.learnMoreUrl ? (
                  <Button
                    component={Link}
                    variant="outlined"
                    href={feature.learnMoreUrl}
                  >
                    Learn More
                  </Button>
                ) : (
                  <Button
                    component={Link}
                    variant={'outlined'}
                    href={'/account/register'}
                  >
                    {t('try_for_free')}
                  </Button>
                )}
              </Grid>
              <Grid
                item
                xs={12}
                md={6}
                order={{ xs: 1, md: index % 2 === 0 ? 2 : 1 }}
              >
                {/*<img*/}
                {/*  src={feature.imageUrl}*/}
                {/*  alt={feature.title}*/}
                {/*  style={{ width: '100%' }}*/}
                {/*/>*/}
                <Typography color={theme.palette.primary.main} fontSize={70}>
                  {(index + 1).toString().padStart(2, '0')}
                </Typography>
              </Grid>
            </Grid>
          ))}
          {children}
        </Container>

        {/* Testimonials */}
        {false && (
          <Box
            sx={{
              py: 4
            }}
          >
            <Container maxWidth="lg">
              <Typography variant="h2" align="center" mb={3}>
                Hear it from our customers
              </Typography>
              <Grid container spacing={4}>
                {testimonials.map((testimonial, index) => (
                  <Grid item xs={12} md={6} key={index}>
                    <Card>
                      <CardContent>
                        <Typography variant="body1" paragraph>
                          "{testimonial.text}"
                        </Typography>
                        <Typography variant="subtitle1" align="right">
                          - {testimonial.author}, {testimonial.company}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            </Container>
          </Box>
        )}
        <TwoCallToActions />
        {/* FAQ */}
        <Container maxWidth="lg">
          <FaqComponent
            title="FAQ"
            items={faqs.map((faq) => ({
              title: faq.question,
              content: <Typography variant="body1">{faq.answer}</Typography>
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
                  <Card sx={{ height: '100%' }}>
                    <img
                      src={content.imageUrl}
                      alt={content.title}
                      style={{ width: '100%' }}
                    />
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
