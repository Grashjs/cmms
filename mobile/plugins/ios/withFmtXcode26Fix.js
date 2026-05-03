const { withDangerousMod } = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const PATCH_MARKER = 'Xcode 26.4 workaround for React Native fmt 11.0.2 consteval errors';

const fmtPatch = `
    # ${PATCH_MARKER}
    installer.pods_project.targets.each do |target|
      next unless target.name == 'fmt'

      target.build_configurations.each do |config|
        config.build_settings['CLANG_CXX_LANGUAGE_STANDARD'] = 'c++17'
      end
    end
`;

function insertFmtPatch(podfile) {
  if (podfile.includes(PATCH_MARKER)) {
    return podfile;
  }

  const preferredAnchor = '\n\n    # This is necessary for Xcode 14';
  if (podfile.includes(preferredAnchor)) {
    return podfile.replace(preferredAnchor, `${fmtPatch}${preferredAnchor}`);
  }

  const postInstallEnd = '\n  end\nend';
  const postInstallEndIndex = podfile.lastIndexOf(postInstallEnd);
  if (postInstallEndIndex !== -1) {
    return `${podfile.slice(0, postInstallEndIndex)}${fmtPatch}${podfile.slice(postInstallEndIndex)}`;
  }

  throw new Error('Unable to insert fmt Xcode 26.4 workaround into ios/Podfile.');
}

module.exports = function withFmtXcode26Fix(config) {
  return withDangerousMod(config, [
    'ios',
    async config => {
      const podfilePath = path.join(config.modRequest.platformProjectRoot, 'Podfile');

      if (!fs.existsSync(podfilePath)) {
        return config;
      }

      const podfile = fs.readFileSync(podfilePath, 'utf8');
      const patchedPodfile = insertFmtPatch(podfile);

      if (patchedPodfile !== podfile) {
        fs.writeFileSync(podfilePath, patchedPodfile);
      }

      return config;
    }
  ]);
};
