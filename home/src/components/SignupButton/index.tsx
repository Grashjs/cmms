"use client";

import { Button, ButtonProps } from "@mui/material";
import { useLocale } from "next-intl";
import { enrichWithClientParams, getSignupUrl } from "src/utils/urlPaths";

interface SignupButtonProps extends ButtonProps {
  params?: Record<string, string>;
}

export default function SignupButton({ params, ...props }: SignupButtonProps) {
  const locale = useLocale();
  const serverSignupUrl = getSignupUrl(locale, params);

  const handleClick = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    e.preventDefault();
    props.onClick?.(e);
    window.location.href = enrichWithClientParams(serverSignupUrl);
  };
  return <Button component="a" variant={"contained"} href={serverSignupUrl} {...props} onClick={handleClick} />;
}
