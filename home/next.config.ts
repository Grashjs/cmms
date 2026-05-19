import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import { codeInspectorPlugin } from "code-inspector-plugin";

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

const nextConfig: NextConfig = {
  /* config options here */
  turbopack: {
    rules: codeInspectorPlugin({
      bundler: "turbopack",
      editor: "idea",
      hotKeys: ["altKey"],
    }),
  },
  experimental: {
    staleTimes: {
      static: 3600, //1 hour
    },
  },
  async rewrites() {
    return [
      {
        source: "/",
        destination: "/en",
      },
      {
        source: "/free-cmms",
        destination: "/en/free-cmms",
      },
      {
        source: "/deletion-policy",
        destination: "/en/deletion-policy",
      },
      {
        source: "/:path((?!en|es|fr|de|tr|pt-br|pl|ar|it|sv|ru|hu|nl|zh-cn|ba|ja|api|_next|_vercel).*)",
        destination: "/en/:path*",
      },
    ];
  },
};

export default withNextIntl(nextConfig);
