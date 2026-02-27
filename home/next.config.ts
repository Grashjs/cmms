import type { NextConfig } from "next";
import createNextIntlPlugin from 'next-intl/plugin';
import { codeInspectorPlugin } from 'code-inspector-plugin';

const withNextIntl = createNextIntlPlugin('./src/i18n/request.ts');

const nextConfig: NextConfig = {
  /* config options here */
    turbopack: {
        rules: codeInspectorPlugin({
            bundler: 'turbopack',
            editor: 'idea',
            hotKeys: ['altKey'],
        }),
    },
};

export default withNextIntl(nextConfig);
