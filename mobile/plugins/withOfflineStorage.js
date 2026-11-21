const { withAndroidManifest, withDangerousMod, withXcodeProject, withAppDelegate, withPodfile, withMainApplication } = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const backupRules = `<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
  <exclude domain="database" path="." />
  <exclude domain="file" path="atlas-offline/" />
  <exclude domain="sharedpref" path="SecureStore" />
</full-backup-content>
`;
const extractionRules = `<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
  <cloud-backup>
    <exclude domain="database" path="." />
    <exclude domain="file" path="atlas-offline/" />
    <exclude domain="sharedpref" path="SecureStore" />
  </cloud-backup>
  <device-transfer>
    <exclude domain="database" path="." />
    <exclude domain="file" path="atlas-offline/" />
    <exclude domain="sharedpref" path="SecureStore" />
  </device-transfer>
</data-extraction-rules>
`;
const startupProtection = `    // Atlas private storage: no backups, available only while the device is unlocked.
    let offlineDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("atlas-offline", isDirectory: true)
    try? FileManager.default.createDirectory(at: offlineDirectory, withIntermediateDirectories: true, attributes: [.protectionKey: FileProtectionType.complete])
    try? FileManager.default.setAttributes([.protectionKey: FileProtectionType.complete], ofItemAtPath: offlineDirectory.path)
    var protectedDirectory = offlineDirectory
    var resourceValues = URLResourceValues()
    resourceValues.isExcludedFromBackup = true
    try? protectedDirectory.setResourceValues(resourceValues)
`;

module.exports = function withOfflineStorage(config) {
  config = withPodfile(config, current => {
    if (!current.modResults.contents.includes("pod 'simdjson'")) {
      current.modResults.contents = current.modResults.contents.replace('  use_expo_modules!',
        "  pod 'simdjson', :path => '../node_modules/@nozbe/simdjson'\n  use_expo_modules!");
    }
    return current;
  });
  config = withAndroidManifest(config, current => {
    const application = current.modResults.manifest.application[0].$;
    application['android:fullBackupContent'] = '@xml/atlas_backup_rules';
    application['android:dataExtractionRules'] = '@xml/atlas_data_extraction_rules';
    return current;
  });
  config = withDangerousMod(config, ['android', async current => {
    const xml = path.join(current.modRequest.platformProjectRoot, 'app/src/main/res/xml');
    fs.mkdirSync(xml, { recursive: true });
    fs.writeFileSync(path.join(xml, 'atlas_backup_rules.xml'), backupRules);
    fs.writeFileSync(path.join(xml, 'atlas_data_extraction_rules.xml'), extractionRules);
    const java = path.join(current.modRequest.platformProjectRoot, 'app/src/main/java/com/atlas/offline');
    fs.mkdirSync(java, { recursive: true });
    for (const file of ['AtlasOfflineStorageModule.java', 'AtlasOfflineStoragePackage.java']) {
      fs.copyFileSync(path.join(__dirname, 'native', file), path.join(java, file));
    }
    return current;
  }]);
  config = withMainApplication(config, current => {
    const registration = 'packages.add(new com.atlas.offline.AtlasOfflineStoragePackage());';
    const kotlinRegistration = 'packages.add(com.atlas.offline.AtlasOfflineStoragePackage())';
    if (!current.modResults.contents.includes('com.atlas.offline.AtlasOfflineStoragePackage')) {
      const marker = current.modResults.language === 'java' ? 'return packages;' : 'return packages';
      if (!current.modResults.contents.includes(marker))
        throw new Error('Unable to register AtlasOfflineStorage in MainApplication');
      current.modResults.contents = current.modResults.contents.replace(marker,
        `${current.modResults.language === 'java' ? registration : kotlinRegistration}\n            ${marker}`);
    }
    return current;
  });
  config = withXcodeProject(config, current => {
    const projectName = current.modRequest.projectName;
    const nativeFile = 'AtlasOfflineStorage.m';
    fs.copyFileSync(path.join(__dirname, 'native', nativeFile),
      path.join(current.modRequest.platformProjectRoot, projectName, nativeFile));
    const project = current.modResults;
    if (!project.hasFile(`${projectName}/${nativeFile}`)) {
      const group = project.findPBXGroupKey({ name: projectName });
      project.addSourceFile(`${projectName}/${nativeFile}`, { target: project.getFirstTarget().uuid }, group);
    }
    if (!project.hasFile('QuickLook.framework')) {
      project.addFramework('QuickLook.framework', { target: project.getFirstTarget().uuid });
    }
    for (const reference of Object.values(project.pbxFileReferenceSection())) {
      if (reference && typeof reference === 'object' && reference.explicitFileType === undefined) {
        delete reference.explicitFileType;
      }
    }
    return current;
  });
  return withAppDelegate(config, current => {
    if (!current.modResults.contents.includes('Atlas private storage:')) {
      current.modResults.contents = current.modResults.contents.replace('    let delegate = ReactNativeDelegate()',
        `${startupProtection}\n    let delegate = ReactNativeDelegate()`);
    }
    return current;
  });
};
module.exports.backupRules = backupRules;
module.exports.extractionRules = extractionRules;
module.exports.startupProtection = startupProtection;
