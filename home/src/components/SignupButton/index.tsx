"use client";

import { Button, ButtonProps } from "@mui/material";
import { useLocale } from "next-intl";
import { getSignupUrl } from "src/utils/urlPaths";

interface SignupButtonProps extends ButtonProps {
  params?: Record<string, string>;
}

export default function SignupButton({ params, ...props }: SignupButtonProps) {
  const locale = useLocale();

  return (
    <Button
      component="a"
      variant={'contained'}
      href={getSignupUrl(locale, params)}
      {...props}
    />
  );
}
