"use client";

import { Button, ButtonProps } from "@mui/material";
import { useLocale } from "next-intl";
import { getSignupUrl } from "src/utils/urlPaths";

interface SignupButtonProps extends ButtonProps {
  params?: Record<string, string>;
}

export default function SignupButton({ params, ...props }: SignupButtonProps) {
  const locale = useLocale();

  const handleClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    // document.referrer is guaranteed to be available here, on user interaction
    const url = getSignupUrl(locale, params);
    window.location.href = url;
  };
  return (
    <Button
      component="a"
      variant={'contained'}
      href={getSignupUrl(locale, params)}
      onClick={handleClick}
      {...props}
    />
  );
}
