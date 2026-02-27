import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "../globals.css";
import Providers from "src/components/Providers";
import EmotionRegistry from 'src/lib/EmotionRegistry';
import { NextIntlClientProvider } from 'next-intl';
import { getMessages, getTranslations } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { locales } from 'src/i18n/request';

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export async function generateMetadata({ 
  params 
}: { 
  params: Promise<{ locale: string }> 
}): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });
  
  const languages = locales.reduce((acc, l) => {
    acc[l] = `/${l}`;
    return acc;
  }, {} as Record<string, string>);

  return {
    title: t('main.title') + "Atlas CMMS",
    description: t('home.h3', {brandName: 'Atlas CMMS'}),
    alternates: {
      languages,
    }
  };
}

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export default async function RootLayout({
  children,
  params
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
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
        suppressHydrationWarning
      >
        <NextIntlClientProvider messages={messages} locale={locale}>
          <EmotionRegistry>
            <Providers>
              {children}
            </Providers>
          </EmotionRegistry>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}