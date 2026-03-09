"use client";

import { FlagComponent } from "country-flag-icons/react/1x1";
import { BR, CN, DE, ES, FR, HU, IT, NL, PL, RU, SA, SE, TR, US, BS } from "country-flag-icons/react/3x2";

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
  | "ZH_CN"
  | "BS";

export const supportedLanguages: {
  code: Lowercase<SupportedLanguage>;
  label: string;
  Icon: FlagComponent;
}[] = [
  {
    code: "en",
    label: "English",
    Icon: US,
  },
  {
    code: "fr",
    label: "French",
    Icon: FR,
  },
  {
    code: "es",
    label: "Spanish",
    Icon: ES,
  },
  {
    code: "de",
    label: "German",
    Icon: DE,
  },
  {
    code: "tr",
    label: "Turkish",
    Icon: TR,
  },
  {
    code: "pt_br",
    label: "Portuguese (Brazil)",
    Icon: BR,
  },
  {
    code: "pl",
    label: "Polish",
    Icon: PL,
  },
  {
    code: "ar",
    label: "Arabic",
    Icon: SA,
  },
  {
    code: "it",
    label: "Italian",
    Icon: IT,
  },
  {
    code: "sv",
    label: "Swedish",
    Icon: SE,
  },
  {
    code: "ru",
    label: "Russian",
    Icon: RU,
  },
  {
    code: "hu",
    label: "Hungarian",
    Icon: HU,
  },
  {
    code: "nl",
    label: "Dutch",
    Icon: NL,
  },
  {
    code: "zh_cn",
    label: "Chinese (Simplified)",
    Icon: CN,
  },
  {
    code: "bs",
    label: "Bosnian",
    Icon: BS,
  },
];
