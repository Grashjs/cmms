import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "../globals.css";
import Providers from "src/components/Providers";
import EmotionRegistry from "src/lib/EmotionRegistry";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";
import { locales } from "src/i18n/request";

const inter = Inter({
  weight: "400",
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

import { getBrandServer } from "src/utils/serverBrand";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const brand = await getBrandServer();

  const languages = locales.reduce(
    (acc, l) => {
      acc[l] = `/${l == "en" ? "" : l}`;
      return acc;
    },
    {} as Record<string, string>,
  );

  return {
    title: {
      template: `%s | ${brand.name}`,
      default: brand.name,
    },
    alternates: {
      canonical: "/",
      languages,
    },
  };
}

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export default async function RootLayout({
  children,
  params,
}: Readonly<{
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}>) {
  const { locale } = await params;

  if (!locales.includes(locale as any)) {
    notFound();
  }

  const messages = await getMessages();

  return (
    <html lang={locale} suppressHydrationWarning>
      <body className={`${inter.variable} antialiased`} suppressHydrationWarning>
        <NextIntlClientProvider messages={messages} locale={locale}>
          <EmotionRegistry>
            <Providers>{children}</Providers>
          </EmotionRegistry>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
