import { Box, Container, Grid, Stack, styled, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { GitHub, LinkedIn, Mail, Phone, Sms } from '@mui/icons-material';
import { getFeaturesLinks, getIndustriesLinks } from '../../utils/urlPaths';
import { useTranslation } from 'react-i18next';
import { ReactNode } from 'react';
import { Link } from 'react-router-dom';

const FooterWrapper = styled(Box)(
  ({ theme }) => `
    background: ${theme.colors.alpha.black[100]};
    color: ${theme.colors.alpha.white[70]};
    padding: ${theme.spacing(4)} 0;
`
);

const FooterLink = styled(Link)(
  ({ theme }) => `
    color: ${theme.colors.alpha.white[70]};
    text-decoration: none;

    &:hover {
      color: ${theme.colors.alpha.white[100]};
      text-decoration: underline;
    }
`
);

const FooterALink = styled('a')(
  ({ theme }) => `
    color: ${theme.colors.alpha.white[70]};
    text-decoration: none;

    &:hover {
      color: ${theme.colors.alpha.white[100]};
      text-decoration: underline;
    }
`
);

const SectionHeading = styled(Typography)(
  ({ theme }) => `
    font-weight: ${theme.typography.fontWeightBold};
    color: ${theme.colors.alpha.white[100]};
    margin-bottom: ${theme.spacing(2)};
`
);
interface ContactItem {
  icon: ReactNode;
  text: string;
  onClick?: () => void;
}

interface LinkItem {
  href: string;
  text: string;
}

interface DynamicLinkItem {
  href: string;
  title: string;
}

interface SocialItem {
  href: string;
  icon: ReactNode;
}

interface AppItem {
  href: string;
  image: string;
  alt: string;
}

interface BaseFooterSection {
  title: string;
}

interface ContactSection extends BaseFooterSection {
  type: 'contact';
  items: ContactItem[];
}

interface LinksSection extends BaseFooterSection {
  type: 'links';
  items: LinkItem[];
}

interface DynamicSection extends BaseFooterSection {
  type: 'dynamic';
  items: DynamicLinkItem[];
}

interface SocialSection extends BaseFooterSection {
  type: 'social';
  items: SocialItem[];
}

interface AppsSection extends BaseFooterSection {
  type: 'apps';
  items: AppItem[];
}

type FooterSection =
  | ContactSection
  | LinksSection
  | DynamicSection
  | SocialSection
  | AppsSection;

export function Footer() {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const footerSections: FooterSection[] = [
    {
      title: 'Contact',
      type: 'contact',
      items: [
        {
          icon: <Mail fontSize="small" />,
          text: 'contact@atlas-cmms.com',
          onClick: () =>
            (window.location.href = 'mailto:contact@atlas-cmms.com')
        },
        {
          icon: <Phone fontSize="small" />,
          text: '+212630690050'
        },
        {
          icon: <Sms fontSize="small" />,
          text: '+212630690050'
        }
      ]
    },
    {
      title: 'Company',
      type: 'links',
      items: [
        { href: '/pricing', text: t('pricing') },
        { href: '/privacy', text: 'Privacy Policy' },
        { href: '/terms-of-service', text: 'Terms of Service' }
      ]
    },
    {
      title: t('features'),
      type: 'dynamic',
      items: getFeaturesLinks(t)
    },
    {
      title: t('industries'),
      type: 'dynamic',
      items: getIndustriesLinks(t)
    },
    {
      title: 'Product',
      type: 'links',
      items: [{ href: '/free-cmms', text: 'Free CMMS' }]
    },
    {
      title: 'Follow Us',
      type: 'social',
      items: [
        {
          href: 'https://www.linkedin.com/company/91710999',
          icon: <LinkedIn />
        },
        { href: 'https://github.com/Grashjs/cmms', icon: <GitHub /> }
      ]
    },
    {
      title: 'Mobile apps',
      type: 'apps',
      items: [
        {
          href: 'https://play.google.com/store/apps/details?id=com.atlas.cmms',
          image: '/static/images/overview/playstore-badge.png',
          alt: 'playstore badge'
        },
        {
          href: 'https://apps.apple.com/us/app/atlas-cmms/id6751547284',
          image: '/static/images/overview/app_store_badge.svg.webp',
          alt: 'app store badge'
        }
      ]
    }
  ];

  const renderSectionContent = (section: FooterSection) => {
    switch (section.type) {
      case 'contact':
        return (
          <Stack spacing={2}>
            {section.items.map((item, index) => (
              <Box
                key={index}
                sx={{ cursor: item.onClick ? 'pointer' : 'default' }}
                onClick={item.onClick}
                display="flex"
                alignItems="center"
              >
                {item.icon}
                <Typography variant="body2" sx={{ ml: 1 }}>
                  {item.text}
                </Typography>
              </Box>
            ))}
          </Stack>
        );
      case 'links':
        return (
          <Stack spacing={2}>
            {section.items.map((item, index) => (
              <FooterLink key={index} to={item.href}>
                {item.text}
              </FooterLink>
            ))}
          </Stack>
        );
      case 'dynamic':
        return (
          <Stack spacing={2}>
            {section.items.map((link) => (
              <FooterLink key={link.href} to={link.href}>
                {link.title}
              </FooterLink>
            ))}
          </Stack>
        );
      case 'social':
        return (
          <Stack direction="row" spacing={2}>
            {section.items.map((item, index) => (
              <FooterALink
                key={index}
                href={item.href}
                target="_blank"
                rel="noopener noreferrer"
              >
                {item.icon}
              </FooterALink>
            ))}
          </Stack>
        );
      case 'apps':
        return (
          <Stack spacing={1} direction={{ xs: 'column', lg: 'row' }}>
            {section.items.map((item, index) => (
              <img
                key={index}
                style={{ cursor: 'pointer' }}
                onClick={() => (window.location.href = item.href)}
                width="150px"
                src={item.image}
                alt={item.alt}
              />
            ))}
          </Stack>
        );
      default:
        return null;
    }
  };

  return (
    <FooterWrapper>
      <Container maxWidth="lg">
        <Grid container spacing={4}>
          {footerSections.map((section, index) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={index}>
              <SectionHeading variant="h5">{section.title}</SectionHeading>
              {renderSectionContent(section)}
            </Grid>
          ))}
        </Grid>
        <Box mt={4} textAlign="center">
          <Typography variant="body2">
            © {new Date().getFullYear()} Intelloop. All rights reserved.
          </Typography>
        </Box>
      </Container>
    </FooterWrapper>
  );
}
