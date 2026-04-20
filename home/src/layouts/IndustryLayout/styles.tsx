"use client";
import { Box, Card, styled, Typography } from "@mui/material";

export const StyledKpiValue = styled(Typography)(({ theme }) => ({
  fontSize: 50,
  fontWeight: 600,
  "& .money-icon, .percentage-icon": {
    color: theme.palette.primary.main,
  },
}));

export const StyledAdvantageCard = styled(Card)(({ theme }) => ({
  height: "100%",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  textAlign: "center",
  padding: theme.spacing(3),
  transition: "transform 0.2s",
  "&:hover": {
    transform: "translateY(-5px)",
    boxShadow: theme.shadows[4],
  },
}));

export const StyledAdvantageIconWrapper = styled(Box)(({ theme }) => ({
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 64,
  height: 64,
  borderRadius: "16px",
  backgroundColor: theme.palette.primary.main,
  color: theme.palette.primary.contrastText,
  marginBottom: theme.spacing(3),
  boxShadow: `0 4px 20px 0 ${theme.palette.primary.main}40`,
}));

export const StyledFeatureNumber = styled(Typography)(({ theme }) => ({
  color: theme.palette.primary.main,
  fontSize: 70,
}));
