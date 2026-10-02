// Learn more https://docs.expo.io/guides/customizing-metro
// const { getDefaultConfig } = require('expo/metro-config');
const { getSentryExpoConfig } = require('@sentry/react-native/metro');
const path = require('path');

const config = getSentryExpoConfig(__dirname);

config.transformer.unstable_transformProfile = 'hermes-stable';

// Locally linked Offline Protocol SDK (D6). Mirrors the SDK's examples/demo-app/metro.config.js.
const sdkDir = path.resolve(
  __dirname,
  '../../offline-protocol-sdk/bindings/react-native'
);
const escape = (p) => p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

config.watchFolders = [...(config.watchFolders || []), sdkDir];
config.resolver.extraNodeModules = {
  ...config.resolver.extraNodeModules,
  '@offline-protocol/mesh-sdk': sdkDir,
  react: path.resolve(__dirname, 'node_modules/react'),
  'react-native': path.resolve(__dirname, 'node_modules/react-native')
};
config.resolver.blockList = [
  ...[].concat(config.resolver.blockList || []),
  new RegExp(`^${escape(path.join(sdkDir, 'node_modules/react'))}/.*`),
  new RegExp(`^${escape(path.join(sdkDir, 'node_modules/react-native'))}/.*`)
];

module.exports = config;
