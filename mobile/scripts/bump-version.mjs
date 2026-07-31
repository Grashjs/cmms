import { readFileSync, writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import path from 'path';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const packageJsonPath = path.join(rootDir, 'package.json');
const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
const currentVersion = packageJson.version;

const args = process.argv.slice(2);
const [major, minor, patch] = currentVersion.split('.').map(Number);

let nextVersion;
if (args[0] && /^\d+\.\d+\.\d+$/.test(args[0])) {
  nextVersion = args[0];
} else if (args.includes('--major')) {
  nextVersion = `${major + 1}.0.0`;
} else if (args.includes('--minor')) {
  nextVersion = `${major}.${minor + 1}.0`;
} else {
  nextVersion = `${major}.${minor}.${patch + 1}`;
}

if (nextVersion === currentVersion) {
  console.error(`Version is already ${currentVersion}`);
  process.exit(1);
}

function replaceFirstN(content, from, to, n) {
  let result = '';
  let remaining = content;
  let count = 0;
  while (count < n) {
    const idx = remaining.indexOf(from);
    if (idx === -1) break;
    result += remaining.slice(0, idx) + to;
    remaining = remaining.slice(idx + from.length);
    count++;
  }
  return result + remaining;
}

function updateFile(relPath, transform) {
  const fullPath = path.join(rootDir, relPath);
  const content = readFileSync(fullPath, 'utf8');
  const next = transform(content);
  if (next === content) {
    console.warn(`WARN: nothing to update in ${relPath}`);
    return false;
  }
  writeFileSync(fullPath, next);
  console.log(`updated ${relPath}`);
  return true;
}

updateFile('package.json', (content) =>
  content.replace(`"version": "${currentVersion}"`, `"version": "${nextVersion}"`)
);

updateFile('package-lock.json', (content) =>
  replaceFirstN(content, `"version": "${currentVersion}"`, `"version": "${nextVersion}"`, 2)
);

updateFile('app.config.ts', (content) =>
  content.split(currentVersion).join(nextVersion)
);

updateFile('android/app/build.gradle', (content) =>
  content.replace(`versionName "${currentVersion}"`, `versionName "${nextVersion}"`)
);

updateFile('android/app/src/main/res/values/strings.xml', (content) =>
  content.replace(
    `<string name="expo_runtime_version">${currentVersion}</string>`,
    `<string name="expo_runtime_version">${nextVersion}</string>`
  )
);

updateFile('ios/AtlasCMMS/Info.plist', (content) =>
  content.split(currentVersion).join(nextVersion)
);

updateFile('ios/AtlasCMMS/Supporting/Expo.plist', (content) =>
  content.split(currentVersion).join(nextVersion)
);

console.log(`\nVersion bumped from ${currentVersion} to ${nextVersion}`);
