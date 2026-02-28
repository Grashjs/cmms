import React, { ReactNode } from "react";
import { Box, Button, Container, Grid, Stack, Typography } from "@mui/material";
import InventoryIcon from "@mui/icons-material/Inventory";
import QrCodeScannerIcon from "@mui/icons-material/QrCodeScanner";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import SmartphoneIcon from "@mui/icons-material/Smartphone";
import CameraAltIcon from "@mui/icons-material/CameraAlt";
import CloudDoneIcon from "@mui/icons-material/CloudDone";
import AddTaskIcon from "@mui/icons-material/AddTask";
import CollectionsIcon from "@mui/icons-material/Collections";
import SpeedIcon from "@mui/icons-material/Speed";
import HistoryIcon from "@mui/icons-material/History";
import QueryStatsIcon from "@mui/icons-material/QueryStats";
import RuleIcon from "@mui/icons-material/Rule";
import { getLocale, getTranslations } from "next-intl/server";
import { getSignupUrl } from "src/utils/urlPaths";
import CompanyLogos from "src/components/CompanyLogos";
import { FeatureRow, ImageWrapper, FeatureContent } from "./styled";
import Image from "next/image";

interface FeaturePoint {
  icon: ReactNode;
  text: string;
}

interface Feature {
  imageSizes: { width: number; height: number };
  title: string;
  points: FeaturePoint[];
  image: string;
  imageAlt: string;
}

export const FeaturesAlternating = async () => {
  const t = await getTranslations();
  const locale = await getLocale();
  const features: Feature[] = [
    {
      title: t("free_cmms.features.work_orders.title"),
      points: [
        {
          icon: <AddTaskIcon color="primary" />,
          text: t("free_cmms.features.work_orders.p1"),
        },
        {
          icon: <CollectionsIcon color="primary" />,
          text: t("free_cmms.features.work_orders.p2"),
        },
        {
          icon: <SpeedIcon color="primary" />,
          text: t("free_cmms.features.work_orders.p3"),
        },
      ],
      image: "/static/images/overview/work_order_screenshot.png",
      imageAlt: t("free_cmms.features.work_orders.alt"),
      imageSizes: { width: 1920, height: 922 },
    },
    {
      title: t("free_cmms.features.asset_tracking.title"),
      points: [
        {
          icon: <HistoryIcon color="primary" />,
          text: t("free_cmms.features.asset_tracking.p1"),
        },
        {
          icon: <QueryStatsIcon color="primary" />,
          text: t("free_cmms.features.asset_tracking.p2"),
        },
        {
          icon: <RuleIcon color="primary" />,
          text: t("free_cmms.features.asset_tracking.p3"),
        },
      ],
      image: "/static/images/features/asset-hero.png",
      imageAlt: t("free_cmms.features.asset_tracking.alt"),
      imageSizes: { width: 1920, height: 912 },
    },
    {
      title: t("free_cmms.features.inventory.title"),
      points: [
        {
          icon: <InventoryIcon color="primary" />,
          text: t("free_cmms.features.inventory.p1"),
        },
        {
          icon: <QrCodeScannerIcon color="primary" />,
          text: t("free_cmms.features.inventory.p2"),
        },
        {
          icon: <NotificationsActiveIcon color="primary" />,
          text: t("free_cmms.features.inventory.p3"),
        },
      ],
      image: "/static/images/overview/inventory_screenshot.png",
      imageAlt: t("free_cmms.features.inventory.alt"),
      imageSizes: { width: 1920, height: 912 },
    },
    {
      title: t("free_cmms.features.mobile.title"),
      points: [
        {
          icon: <SmartphoneIcon color="primary" />,
          text: t("free_cmms.features.mobile.p1"),
        },
        {
          icon: <CameraAltIcon color="primary" />,
          text: t("free_cmms.features.mobile.p2"),
        },
        {
          icon: <CloudDoneIcon color="primary" />,
          text: t("free_cmms.features.mobile.p3"),
        },
      ],
      image: "/static/mobile_app.jpeg",
      imageAlt: t("free_cmms.features.mobile.alt"),
      imageSizes: { width: 720, height: 1600 },
    },
  ];

  const isPortrait = (sizes: { width: number; height: number }) => sizes.height > sizes.width;

  return (
    <Container maxWidth="lg" sx={{ mt: 6 }}>
      {features.map((feature, index) => {
        const isEven = index % 2 === 0;
        const portrait = isPortrait(feature.imageSizes);
        return (
          <FeatureRow container key={index} direction={isEven ? "row" : "row-reverse"}>
            <Grid item xs={12} md={6}>
              <ImageWrapper
                sx={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                }}
              >
                <Image
                  src={feature.image}
                  alt={feature.imageAlt}
                  width={feature.imageSizes.width}
                  height={feature.imageSizes.height}
                  style={{
                    borderRadius: "16px",
                    width: "auto",
                    height: "auto",
                    maxHeight: "500px",
                    maxWidth: "100%",
                  }}
                />
              </ImageWrapper>
            </Grid>
            <Grid item xs={12} md={6}>
              <FeatureContent>
                <Typography
                  variant="h2"
                  gutterBottom
                  sx={{
                    fontWeight: 700,
                    fontSize: { xs: "2rem", md: "2.5rem" },
                    color: "text.primary",
                    mb: 3,
                  }}
                >
                  {feature.title}
                </Typography>
                <Stack spacing={2} sx={{ mb: 4 }}>
                  {feature.points.map((point, pIndex) => (
                    <Box
                      key={pIndex}
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: { xs: "center", md: "flex-start" },
                        textAlign: "left",
                      }}
                    >
                      <Box sx={{ mr: 2, display: "flex", flexShrink: 0 }}>{point.icon}</Box>
                      <Typography
                        variant="body1"
                        sx={{
                          fontSize: "1.1rem",
                          color: "text.secondary",
                        }}
                      >
                        {point.text}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
                <Button
                  component={"a"}
                  href={getSignupUrl(locale)}
                  variant="contained"
                  size="large"
                  sx={{
                    px: 4,
                    py: 1.5,
                    fontSize: "1rem",
                    borderRadius: "50px",
                    textTransform: "none",
                    fontWeight: "bold",
                  }}
                >
                  {t("free_cmms.features.get_started_free")}
                </Button>
              </FeatureContent>
            </Grid>
          </FeatureRow>
        );
      })}

      <CompanyLogos />
      <Box
        sx={{
          textAlign: "center",
          borderRadius: 2,
          my: 10,
          px: 4,
        }}
      >
        <Typography variant="h2" gutterBottom sx={{ fontWeight: 800 }}>
          {t("free_cmms.features.ready_to_optimize")}
        </Typography>
        <Typography variant="h5" sx={{ mb: 4, opacity: 0.9 }}>
          {t("free_cmms.features.join_thousands")}
        </Typography>
        <Button
          component={"a"}
          href={getSignupUrl(locale)}
          variant="contained"
          size="large"
          sx={{
            px: 6,
            py: 2,
            fontSize: "1.2rem",
            borderRadius: "50px",
            textTransform: "none",
            fontWeight: "bold",
            boxShadow: "0 10px 20px rgba(0,0,0,0.2)",
          }}
        >
          {t("free_cmms.features.get_started_no_card")}
        </Button>
      </Box>
    </Container>
  );
};

export default FeaturesAlternating;
