"use client";

import { Button, CircularProgress } from "@mui/material";
import React, { useState } from "react";
import api from "src/utils/api";
import { fireGa4Event } from "src/utils/overall";
import { useTranslations } from "next-intl";
import useScrollToLocation from "src/hooks/useScrollToLocation";
import { mainAppUrl } from "src/config";

export default function LiveDemoButton() {
  const t = useTranslations();
  const [generatingAccount, setGeneratingAccount] = useState<boolean>(false);
  useScrollToLocation();

  const onSeeLiveDemo = async () => {
    setGeneratingAccount(true);
    try {
      fireGa4Event("live_demo_view");
      const { success, message } = await api.get<{
        success: boolean;
        message: string;
      }>("demo/generate-account");

      if (success) {
        window.location.href = mainAppUrl + `account/login?token=${message}`;
      } else {
        setGeneratingAccount(false);
      }
    } catch (error) {
      setGeneratingAccount(false);
    }
  };

  return (
    <Button
      sx={{
        ml: 2,
      }}
      component="a"
      startIcon={
        <CircularProgress size={"1rem"} color="primary" sx={{ visibility: generatingAccount ? "visible" : "hidden" }} />
      }
      disabled={generatingAccount}
      onClick={onSeeLiveDemo}
      size="medium"
      variant="text"
    >
      {t("see_live_demo")}
    </Button>
  );
}
