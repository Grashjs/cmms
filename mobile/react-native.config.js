module.exports = {
  dependencies: {
    // Disable native iOS autolinking for the NFC manager while keeping Android support.
    'react-native-nfc-manager': {
      platforms: {
        ios: null
      }
    }
  }
};
