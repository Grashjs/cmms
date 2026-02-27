"use client";

import { FlagComponent } from "country-flag-icons/react/1x1";
import { BR, CN, DE, ES, FR, HU, IT, NL, PL, RU, SA, SE, TR, US } from "country-flag-icons/react/3x2";
import { LocaleSingularArg } from "@fullcalendar/react";
import deLocale from "@fullcalendar/core/locales/de";
import esLocale from "@fullcalendar/core/locales/es";
import arLocale from "@fullcalendar/core/locales/ar";
import trLocale from "@fullcalendar/core/locales/tr";
import ptBRLocale from "@fullcalendar/core/locales/pt-br";
import frLocale from "@fullcalendar/core/locales/fr";
import plLocale from "@fullcalendar/core/locales/pl";
import enLocale from "@fullcalendar/core/locales/en-gb";
import itLocale from "@fullcalendar/core/locales/it";
import svLocale from "@fullcalendar/core/locales/sv";
import ruLocale from "@fullcalendar/core/locales/ru";
import huLocale from "@fullcalendar/core/locales/hu";
import nlLocale from "@fullcalendar/core/locales/nl";
import zhCNLocale from "@fullcalendar/core/locales/zh-cn";
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
  calendarLocale: LocaleSingularArg;
  dateLocale: DateLocale;
}[] = [
  {
    code: "en",
    label: "English",
    Icon: US,
    calendarLocale: enLocale,
    dateLocale: enUS,
  },
  {
    code: "fr",
    label: "French",
    Icon: FR,
    calendarLocale: frLocale,
    dateLocale: fr,
  },
  {
    code: "es",
    label: "Spanish",
    Icon: ES,
    calendarLocale: esLocale,
    dateLocale: es,
  },
  {
    code: "de",
    label: "German",
    Icon: DE,
    calendarLocale: deLocale,
    dateLocale: de,
  },
  {
    code: "tr",
    label: "Turkish",
    Icon: TR,
    calendarLocale: trLocale,
    dateLocale: tr,
  },
  {
    code: "pt_br",
    label: "Portuguese (Brazil)",
    Icon: BR,
    calendarLocale: ptBRLocale,
    dateLocale: ptBR,
  },
  {
    code: "pl",
    label: "Polish",
    Icon: PL,
    calendarLocale: plLocale,
    dateLocale: pl,
  },
  {
    code: "ar",
    label: "Arabic",
    Icon: SA,
    calendarLocale: arLocale,
    dateLocale: ar,
  },
  {
    code: "it",
    label: "Italian",
    Icon: IT,
    calendarLocale: itLocale,
    dateLocale: it,
  },
  {
    code: "sv",
    label: "Swedish",
    Icon: SE,
    calendarLocale: svLocale,
    dateLocale: sv,
  },
  {
    code: "ru",
    label: "Russian",
    Icon: RU,
    calendarLocale: ruLocale,
    dateLocale: ru,
  },
  {
    code: "hu",
    label: "Hungarian",
    Icon: HU,
    calendarLocale: huLocale,
    dateLocale: hu,
  },
  {
    code: "nl",
    label: "Dutch",
    Icon: NL,
    calendarLocale: nlLocale,
    dateLocale: nl,
  },
  {
    code: "zh_cn",
    label: "Chinese (Simplified)",
    Icon: CN,
    calendarLocale: zhCNLocale,
    dateLocale: zhCN,
  },
];
