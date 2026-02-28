import { Inter } from "next/font/google";
import "../globals.css";
import Providers from "src/components/Providers";
import EmotionRegistry from "src/components/EmotionRegistry";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";
import { locales } from "src/i18n/request";
import { BrandProvider } from "src/contexts/BrandContext";
import { getBrandServer } from "src/utils/serverBrand";
import { Metadata } from "next";
import { GoogleAnalytics } from "@next/third-parties/google";
import { googleTrackingId } from "src/config";

const inter = Inter({
  weight: "400",
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });

  return {
    title: {
      default: "Atlas CMMS",
      template: "%s | Atlas CMMS", // page title fills %s
    },
    metadataBase: new URL("https://atlas-cmms.com"),
    icons: {
      icon: [
        { url: "/favicon.ico" },
        { url: "/favicon-16x16.png", sizes: "16x16", type: "image/png" },
        { url: "/favicon-32x32.png", sizes: "32x32", type: "image/png" },
      ],
    },
  };
}

export default async function RootLayout({
  children,
  params,
}: Readonly<{
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}>) {
  const { locale } = await params;

  if (!locales.includes(locale)) {
    notFound();
  }
  const brand = await getBrandServer();

  const messages = await getMessages();

  return (
    <html lang={locale} suppressHydrationWarning>
      <body className={`${inter.variable} antialiased`} suppressHydrationWarning>
        {googleTrackingId && <GoogleAnalytics gaId={googleTrackingId} />}
        <NextIntlClientProvider messages={messages} locale={locale}>
          <BrandProvider brand={brand}>
            <EmotionRegistry>
              <Providers>{children}</Providers>
            </EmotionRegistry>
          </BrandProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
