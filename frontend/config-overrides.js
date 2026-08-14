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
