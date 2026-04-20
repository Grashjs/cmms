"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getWorkOrdersUrl } from "src/utils/urlPaths";
import { CircularProgress } from "@mui/material";

const PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.atlas.cmms";
const APP_STORE_URL = "https://apps.apple.com/us/app/atlas-cmms/id6751547284";

export default function MbAppPage() {
  const router = useRouter();

  useEffect(() => {
    const userAgent = navigator.userAgent.toLowerCase();
    const isAndroid = /android/.test(userAgent);
    const isIOS = /iphone|ipad|ipod/.test(userAgent);

    if (isAndroid) {
      window.location.href = PLAY_STORE_URL;
    } else if (isIOS) {
      window.location.href = APP_STORE_URL;
    } else {
      window.location.href = getWorkOrdersUrl("en");
    }
  }, [router]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <CircularProgress size={"2rem"} />
    </div>
  );
}
