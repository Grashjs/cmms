"use client";

import { FlagComponent } from "country-flag-icons/react/1x1";
import { BR, CN, DE, ES, FR, HU, IT, NL, PL, RU, SA, SE, TR, US } from "country-flag-icons/react/3x2";

import { Locale as DateLocale } from "date-fns";
import { ar, de, enUS, es, fr, hu, it, nl, pl, ptBR, ru, sv, tr, zhCN } from "date-fns/locale";

export type SupportedLanguage =
  | "DE"
  | "EN"
  | "FR"
  | "TR"
  | "ES"
  | "PT_BR"
  | "PL"
  | "IT"
  | "SV"
  | "RU"
  | "AR"
  | "HU"
  | "NL"
  | "ZH_CN";

export const supportedLanguages: {
  code: Lowercase<SupportedLanguage>;
  label: string;
  Icon: FlagComponent;
  dateLocale: DateLocale;
}[] = [
  {
    code: "en",
    label: "English",
    Icon: US,
    dateLocale: enUS,
  },
  {
    code: "fr",
    label: "French",
    Icon: FR,
    dateLocale: fr,
  },
  {
    code: "es",
    label: "Spanish",
    Icon: ES,
    dateLocale: es,
  },
  {
    code: "de",
    label: "German",
    Icon: DE,
    dateLocale: de,
  },
  {
    code: "tr",
    label: "Turkish",
    Icon: TR,
    dateLocale: tr,
  },
  {
    code: "pt_br",
    label: "Portuguese (Brazil)",
    Icon: BR,
    dateLocale: ptBR,
  },
  {
    code: "pl",
    label: "Polish",
    Icon: PL,
    dateLocale: pl,
  },
  {
    code: "ar",
    label: "Arabic",
    Icon: SA,
    dateLocale: ar,
  },
  {
    code: "it",
    label: "Italian",
    Icon: IT,
    dateLocale: it,
  },
  {
    code: "sv",
    label: "Swedish",
    Icon: SE,
    dateLocale: sv,
  },
  {
    code: "ru",
    label: "Russian",
    Icon: RU,
    dateLocale: ru,
  },
  {
    code: "hu",
    label: "Hungarian",
    Icon: HU,
    dateLocale: hu,
  },
  {
    code: "nl",
    label: "Dutch",
    Icon: NL,
    dateLocale: nl,
  },
  {
    code: "zh_cn",
    label: "Chinese (Simplified)",
    Icon: CN,
    dateLocale: zhCN,
  },
];
