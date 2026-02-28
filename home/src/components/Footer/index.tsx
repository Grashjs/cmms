import { Box, Container, Grid, Stack, Typography } from "@mui/material";
import { GitHub, LinkedIn, Mail, Phone, Sms } from "@mui/icons-material";
import { getFeaturesLinks, getIndustriesLinks } from "src/utils/urlPaths";
import { getTranslations } from "next-intl/server";
import { ReactNode } from "react";
import { FooterAnchor, FooterLink, FooterWrapper, SectionHeading } from "./styles";
import Image from "next/image";

interface ContactItem {
  icon: ReactNode;
  text: string;
  href?: string;
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
  type: "contact";
  items: ContactItem[];
}

interface LinksSection extends BaseFooterSection {
  type: "links";
  items: LinkItem[];
}

interface DynamicSection extends BaseFooterSection {
  type: "dynamic";
  items: DynamicLinkItem[];
}

interface SocialSection extends BaseFooterSection {
  type: "social";
  items: SocialItem[];
}

interface AppsSection extends BaseFooterSection {
  type: "apps";
  items: AppItem[];
}

type FooterSection = ContactSection | LinksSection | DynamicSection | SocialSection | AppsSection;

export default async function Footer() {
  const t = await getTranslations();

  const footerSections: FooterSection[] = [
    {
      title: "Contact",
      type: "contact",
      items: [
        {
          icon: <Mail fontSize="small" />,
          text: "contact@atlas-cmms.com",
          href: "mailto:contact@atlas-cmms.com",
        },
        {
          icon: <Phone fontSize="small" />,
          text: "+212630690050",
          href: "tel:+212630690050",
        },
        {
          icon: <Sms fontSize="small" />,
          text: "+212630690050",
          href: "sms:+212630690050",
        },
      ],
    },
    {
      title: "Company",
      type: "links",
      items: [
        { href: "/pricing", text: t("pricing") },
        { href: "/privacy", text: "Privacy Policy" },
        { href: "/terms-of-service", text: "Terms of Service" },
      ],
    },
    {
      title: t("features"),
      type: "dynamic",
      items: getFeaturesLinks(t),
    },
    {
      title: t("industries"),
      type: "dynamic",
      items: getIndustriesLinks(t),
    },
    {
      title: "Product",
      type: "links",
      items: [{ href: "/free-cmms", text: "Free CMMS" }],
    },
    {
      title: "Follow Us",
      type: "social",
      items: [
        {
          href: "https://www.linkedin.com/company/91710999",
          icon: <LinkedIn />,
        },
        { href: "https://github.com/Grashjs/cmms", icon: <GitHub /> },
      ],
    },
    {
      title: "Mobile apps",
      type: "apps",
      items: [
        {
          href: "https://play.google.com/store/apps/details?id=com.atlas.cmms",
          image: "/static/images/overview/playstore-badge.png",
          alt: "playstore badge",
        },
        {
          href: "https://apps.apple.com/us/app/atlas-cmms/id6751547284",
          image: "/static/images/overview/app_store_badge.svg.webp",
          alt: "app store badge",
        },
      ],
    },
  ];

  const renderSectionContent = (section: FooterSection) => {
    switch (section.type) {
      case "contact":
        return (
          <Stack spacing={2}>
            {section.items.map((item, index) => {
              const content = (
                <>
                  {item.icon}
                  <Typography variant="body2" sx={{ ml: 1 }}>
                    {item.text}
                  </Typography>
                </>
              );

              if (item.href) {
                return (
                  <FooterAnchor key={index} href={item.href} sx={{ display: "flex", alignItems: "center" }}>
                    {content}
                  </FooterAnchor>
                );
              }

              return (
                <Box key={index} display="flex" alignItems="center">
                  {content}
                </Box>
              );
            })}
          </Stack>
        );
      case "links":
        return (
          <Stack spacing={2}>
            {section.items.map((item, index) => (
              <FooterLink key={index} href={item.href}>
                {item.text}
              </FooterLink>
            ))}
          </Stack>
        );
      case "dynamic":
        return (
          <Stack spacing={2}>
            {section.items.map((link) => (
              <FooterLink key={link.href} href={link.href}>
                {link.title}
              </FooterLink>
            ))}
          </Stack>
        );
      case "social":
        return (
          <Stack direction="row" spacing={2}>
            {section.items.map((item, index) => (
              <FooterAnchor key={index} href={item.href} target="_blank" rel="noopener noreferrer">
                {item.icon}
              </FooterAnchor>
            ))}
          </Stack>
        );
      case "apps":
        return (
          <Stack spacing={1} direction={{ xs: "column", lg: "row" }}>
            {section.items.map((item, index) => (
              <a key={index} href={item.href} target="_blank" rel="noopener noreferrer">
                <Image width="150" src={item.image} alt={item.alt} />
              </a>
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
          <Typography variant="body2" suppressHydrationWarning>
            © {new Date().getFullYear()} Intelloop. All rights reserved.
          </Typography>
        </Box>
      </Container>
    </FooterWrapper>
  );
}
