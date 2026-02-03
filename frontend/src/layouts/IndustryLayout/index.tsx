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
import { Helmet } from 'react-helmet-async';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { demoLink } from '../../config';
import { OverviewWrapper } from '../../content/landing';
import { TypographyH2 } from '../../content/landing/HeroFree';

interface Feature {
  title: string;
  description: string;
  imageUrl: string;
  learnMoreUrl: string;
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

interface IndustryLayoutProps {
  pageTitle: string;
  headerTitle: string;
  headerSubtitle: string;
  headerImageUrl: string;
  companyLogos: string[];
  features: Feature[];
  testimonials: Testimonial[];
  faqs: FAQ[];
  relatedContent: RelatedContent[];
}

const IndustryLayout: FC<IndustryLayoutProps> = (props) => {
  const {
    pageTitle,
    headerTitle,
    headerSubtitle,
    headerImageUrl,
    companyLogos,
    features,
    testimonials,
    faqs,
    relatedContent
  } = props;
  const { t } = useTranslation();
  const theme = useTheme();

  return (
    <OverviewWrapper>
      <Helmet>
        <title>{pageTitle}</title>
      </Helmet>
      <NavBar />
      <Box>
        {/* Header */}
        <Container maxWidth="lg">
          <Stack direction={'row'} spacing={4} justifyContent={'space-between'}>
            <Box>
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
                  component={RouterLink}
                  to="/account/register"
                >
                  {t('try_for_free')}
                </Button>
                <Button size="large" href={demoLink} variant="outlined">
                  {t('book_demo')}
                </Button>
              </Stack>
            </Box>
            <Box
              component="img"
              src={headerImageUrl}
              alt={headerTitle}
              sx={{ width: '100%' }}
            />
          </Stack>
        </Container>

        {/* Company Logos */}
        <Container maxWidth="lg" sx={{ py: 5 }}>
          <Grid container spacing={4} justifyContent="center">
            {companyLogos.map((logo, index) => (
              <Grid item key={index}>
                <img src={logo} alt={`company-logo-${index}`} height="40" />
              </Grid>
            ))}
          </Grid>
        </Container>

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
                <Button variant="outlined" href={feature.learnMoreUrl}>
                  Learn More
                </Button>
              </Grid>
              <Grid
                item
                xs={12}
                md={6}
                order={{ xs: 1, md: index % 2 === 0 ? 2 : 1 }}
              >
                <img
                  src={feature.imageUrl}
                  alt={feature.title}
                  style={{ width: '100%' }}
                />
              </Grid>
            </Grid>
          ))}
        </Container>

        {/* Testimonials */}
        <Box
          sx={{
            py: 8
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

        {/* FAQ */}
        <Container maxWidth="lg" sx={{ py: 8 }}>
          <Typography variant="h2" align="center" gutterBottom>
            FAQ
          </Typography>
          {faqs.map((faq, index) => (
            <Box key={index} sx={{ mb: 2 }}>
              <Typography variant="h6">{faq.question}</Typography>
              <Typography variant="body1">{faq.answer}</Typography>
            </Box>
          ))}
        </Container>

        {/* Related Content */}
        <Container maxWidth="lg" sx={{ py: 8 }}>
          <Typography variant="h2" align="center" gutterBottom>
            Related Content
          </Typography>
          <Grid container spacing={4}>
            {relatedContent.map((content, index) => (
              <Grid item xs={12} md={4} key={index}>
                <Card>
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
      </Box>
      <Footer />
    </OverviewWrapper>
  );
};

export default IndustryLayout;
