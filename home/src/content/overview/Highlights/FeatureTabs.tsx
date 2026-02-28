"use client";
import { useTranslations } from "next-intl";
import { SetStateAction, useState } from "react";
import { Box, Grid, List, ListItem, ListItemText, Tab, Tabs, Typography, Container } from "@mui/material";
import CheckTwoToneIcon from "@mui/icons-material/CheckTwoTone";
import { BrandConfig } from "src/utils/serverBrand";
import {
  AvatarSuccess,
  Blob1,
  Blob2,
  BlowWrapper,
  CardImageWrapper,
  TypographyH1Primary,
  TypographyH2,
} from "./styles";
import Image from "next/image";

export default function FeatureTabs({ brandConfig }: { brandConfig: BrandConfig }) {
  const t = useTranslations();
  const [currentTab, setCurrentTab] = useState("work-orders");

  const tabs = [
    { value: "work-orders", label: t("work_orders") },
    { value: "request", label: t("request_system") },
    { value: "mobile", label: t("mobile_app") },
    { value: "asset", label: t("asset_management") },
    { value: "preventative", label: t("preventive_maintenance") },
    { value: "part", label: t("parts_inventory") },
    { value: "dashboard", label: t("custom_dashboards") },
  ];

  const featuresConfiguration: {
    [key: string]: {
      title: { key: string; params?: Record<string, any> };
      descriptions: { key: string; params?: Record<string, any> }[];
      checks: { key: string; params?: Record<string, any> }[];
      image: { src: string; width: number; height: number };
    };
  } = {
    "work-orders": {
      title: {
        key: "work-orders.title",
      },
      descriptions: [
        { key: "work-orders.descriptions.0" },
        {
          key: "work-orders.descriptions.1",
          params: { shortBrandName: brandConfig.name },
        },
      ],
      checks: [
        { key: "work-orders.checks.0" },
        { key: "work-orders.checks.1" },
        { key: "work-orders.checks.2" },
        { key: "work-orders.checks.3" },
        {
          key: "work-orders.checks.4",
          params: { shortBrandName: brandConfig.name },
        },
      ],
      image: { src: "/static/images/overview/work_order_screenshot.png", width: 1920, height: 922 },
    },

    request: {
      title: {
        key: "work-requests.title",
      },
      descriptions: [
        { key: "work-requests.descriptions.0" },
        {
          key: "work-requests.descriptions.1",
          params: { shortBrandName: brandConfig.shortName },
        },
      ],
      checks: [{ key: "work-requests.checks.0" }, { key: "work-requests.checks.1" }, { key: "work-requests.checks.2" }],
      image: { src: "/static/images/overview/request.png", width: 1920, height: 869 },
    },

    mobile: {
      title: { key: "mobile-app.title" },
      descriptions: [{ key: "mobile-app.descriptions.0" }, { key: "mobile-app.descriptions.1" }],
      checks: [
        { key: "mobile-app.checks.0" },
        { key: "mobile-app.checks.1" },
        { key: "mobile-app.checks.2" },
        { key: "mobile-app.checks.3" },
        { key: "mobile-app.checks.4" },
        { key: "mobile-app.checks.5" },
      ],
      image: { src: "/static/images/overview/mobile_home.png", width: 736, height: 736 },
    },

    asset: {
      title: { key: "eam.title" },
      descriptions: [
        { key: "eam.descriptions.0" },
        {
          key: "eam.descriptions.1",
          params: { brandName: brandConfig.shortName },
        },
      ],
      checks: [
        { key: "eam.checks.0" },
        { key: "eam.checks.1" },
        { key: "eam.checks.2" },
        { key: "eam.checks.3" },
        { key: "eam.checks.4" },
        { key: "eam.checks.5" },
      ],
      image: { src: "/static/images/overview/assets.png", width: 1920, height: 869 },
    },

    preventative: {
      title: {
        key: "pm.title",
      },
      descriptions: [
        { key: "pm.descriptions.0" },
        {
          key: "pm.descriptions.1",
          params: { shortBrandName: brandConfig.shortName },
        },
      ],
      checks: [
        { key: "pm.checks.0" },
        { key: "pm.checks.1" },
        { key: "pm.checks.2" },
        { key: "pm.checks.3" },
        { key: "pm.checks.4" },
        { key: "pm.checks.5" },
      ],
      image: { src: "/static/images/overview/pm.png", width: 1920, height: 869 },
    },

    part: {
      title: {
        key: "part_1.title",
      },
      descriptions: [
        { key: "part_1.descriptions.0" },
        {
          key: "part_1.descriptions.1",
          params: { shortBrandName: brandConfig.shortName },
        },
      ],
      checks: [
        { key: "part_1.checks.0" },
        { key: "part_1.checks.1" },
        { key: "part_1.checks.2" },
        { key: "part_1.checks.3" },
        { key: "part_1.checks.4" },
        { key: "part_1.checks.5" },
        { key: "part_1.checks.6" },
        { key: "part_1.checks.7" },
      ],
      image: { src: "/static/images/overview/inventory_screenshot.png", width: 1882, height: 936 },
    },

    dashboard: {
      title: {
        key: "dashboard.title",
      },
      descriptions: [
        { key: "dashboard.descriptions.0" },
        {
          key: "dashboard.descriptions.1",
          params: { shortBrandName: brandConfig.shortName },
        },
        {
          key: "dashboard.descriptions.2",
          params: { shortBrandName: brandConfig.shortName },
        },
      ],
      checks: [
        { key: "dashboard.checks.0" },
        { key: "dashboard.checks.1" },
        { key: "dashboard.checks.2" },
        { key: "dashboard.checks.3" },
        {
          key: "dashboard.checks.4",
          params: { shortBrandName: brandConfig.shortName },
        },
        { key: "dashboard.checks.5" },
      ],
      image: { src: "/static/images/overview/analytics_screenshot.png", width: 1920, height: 922 },
    },
  };

  const CheckItem = ({ description }: { description: string }) => {
    return (
      <ListItem>
        <AvatarSuccess
          sx={{
            mr: 2,
          }}
        >
          <CheckTwoToneIcon />
        </AvatarSuccess>
        <ListItemText primary={description} />
      </ListItem>
    );
  };

  const Feature = ({
    title,
    descriptions,
    checks,
    image,
  }: {
    title: string;
    descriptions: string[];
    checks: string[];
    image: { src: string; width: number; height: number };
  }) => {
    return (
      <Grid
        sx={{
          mt: 8,
        }}
        container
        spacing={4}
      >
        <Grid item xs={12} md={6}>
          <Typography sx={{ mb: 1 }} variant="h2">
            {title}.
          </Typography>
          {descriptions.map((description, index) => (
            <Box key={index}>
              <Typography variant="subtitle2">{description}</Typography>
              <br />
            </Box>
          ))}
          <List
            disablePadding
            sx={{
              mt: 2,
            }}
          >
            {checks.map((desc, index) => (
              <CheckItem key={index} description={desc} />
            ))}
          </List>
        </Grid>
        <Grid item xs={12} md={6}>
          <BlowWrapper>
            <Blob1 />
            <Blob2 />
            <CardImageWrapper>
              <Image src={image.src} width={image.width} height={image.height} alt={brandConfig.shortName} />
            </CardImageWrapper>
          </BlowWrapper>
        </Grid>
      </Grid>
    );
  };
  const handleTabsChange = (_event: any, value: SetStateAction<string>) => {
    setCurrentTab(value);
  };

  return (
    <>
      <TypographyH1Primary
        id="key-features"
        textAlign="center"
        sx={{
          mt: 8,
          mb: 2,
        }}
        variant="h2"
      >
        {t("key_features")}
      </TypographyH1Primary>
      <Container maxWidth="sm">
        <TypographyH2
          sx={{
            pb: 4,
            lineHeight: 1.5,
          }}
          textAlign="center"
          variant="h4"
          color="text.secondary"
          fontWeight="normal"
        >
          {t("key_features_description", {
            shortBrandName: brandConfig.name,
          })}
        </TypographyH2>
      </Container>
      <Box
        sx={{
          justifyContent: "flex-start",
          ".MuiTabs-scrollableX": {
            overflow: "auto !important",
          },
        }}
      >
        <Tabs
          onChange={handleTabsChange}
          value={currentTab}
          variant="scrollable"
          scrollButtons={true}
          allowScrollButtonsMobile
          textColor="primary"
          indicatorColor="primary"
          sx={{
            "& .MuiTabs-scrollButtons": {
              color: "black", // arrow color
            },
            "& .MuiTabs-scrollButtons.Mui-disabled": {
              opacity: 0.3, // optional: style disabled state
              color: "black",
            },
          }}
        >
          {tabs.map((tab) => (
            <Tab key={tab.value} label={tab.label} value={tab.value} />
          ))}
        </Tabs>
      </Box>
      {Object.entries(featuresConfiguration).map(([feature, config]) => (
        <Box key={feature} sx={{ display: currentTab === feature ? "block" : "none" }}>
          <Feature
            title={t(config.title.key, config.title.params)}
            descriptions={config.descriptions.map((desc) => t(desc.key, desc.params))}
            checks={config.checks.map((check) => t(check.key, check.params))}
            image={config.image}
          />
        </Box>
      ))}
    </>
  );
}
