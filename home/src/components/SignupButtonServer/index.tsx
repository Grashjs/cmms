import { Button, ButtonProps } from "@mui/material";
import { getSignupUrl } from "src/utils/urlPaths";
import { getLocale } from "next-intl/server";

interface SignupButtonProps extends ButtonProps {
  params?: Record<string, string>;
}

export default async function SignupButtonServer({ params, ...props }: SignupButtonProps) {
  const locale = await getLocale();
  const serverSignupUrl = getSignupUrl(locale, params);

  return <Button component="a" variant={"contained"} href={serverSignupUrl} data-utm-enriched {...props} />;
}
