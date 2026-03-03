// webpack.config.js
const { codeInspectorPlugin } = require('code-inspector-plugin');

module.exports = function override(config, env) {
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
};
