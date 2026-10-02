const { withDangerousMod } = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const PATCH_MARKER = 'Xcode 27 workaround: pod targets below iOS 15 fail to build';

// Xcode 27 rejects IPHONEOS_DEPLOYMENT_TARGET < 15.0, and several pods (mostly their
// resource bundle targets) still declare 9.0-13.0. Raise them to the app's 15.1.
const patch = `
    # ${PATCH_MARKER}
    installer.pods_project.targets.each do |target|
      target.build_configurations.each do |config|
        if config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'].to_f < 15.1
          config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '15.1'
        end
      end
    end
`;

function insertPatch(podfile) {
  if (podfile.includes(PATCH_MARKER)) {
    return podfile;
  }

  const anchor = '\n\n    # This is necessary for Xcode 14';
  if (podfile.includes(anchor)) {
    return podfile.replace(anchor, `${patch}${anchor}`);
  }

  throw new Error('Unable to insert Xcode 27 deployment target workaround into ios/Podfile.');
}

module.exports = function withXcode27PodsDeploymentTarget(config) {
  return withDangerousMod(config, [
    'ios',
    async (config) => {
      const podfilePath = path.join(config.modRequest.platformProjectRoot, 'Podfile');

      if (!fs.existsSync(podfilePath)) {
        return config;
      }

      const podfile = fs.readFileSync(podfilePath, 'utf8');
      const patchedPodfile = insertPatch(podfile);

      if (patchedPodfile !== podfile) {
        fs.writeFileSync(podfilePath, patchedPodfile);
      }

      return config;
    }
  ]);
};
