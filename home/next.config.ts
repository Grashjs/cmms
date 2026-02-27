import type { NextConfig } from "next";
import { codeInspectorPlugin } from 'code-inspector-plugin';

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

export default nextConfig;
