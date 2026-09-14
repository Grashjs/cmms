// config-overrides.js
const { codeInspectorPlugin } = require('code-inspector-plugin');

module.exports = {
  webpack: function override(config, env) {
    if (env === 'development') {
      config.plugins.push(
        codeInspectorPlugin({
          bundler: 'webpack',
          editor: 'idea',
          hotKeys: ['altKey']
        })
      );
    }
    if (env === 'production' && process.env.SENTRY_AUTH_TOKEN) {
      const { sentryWebpackPlugin } = require('@sentry/webpack-plugin');
      config.plugins.push(
        sentryWebpackPlugin({
          org: process.env.SENTRY_ORG,
          project: process.env.SENTRY_PROJECT,
          authToken: process.env.SENTRY_AUTH_TOKEN
        })
      );
    }
    return config;
  },

  devServer: function (configFunction) {
    return function (proxy, allowedHost) {
      const config = configFunction(proxy, allowedHost);
      config.client = {
        ...config.client,
        overlay: false
      };

      return config;
    };
  }
};
