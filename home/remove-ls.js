const fs = require('fs');
const path = require('path');

const filesToClean = [
  'src/components/NavBar/index.tsx',
  'src/content/pricing/index.tsx',
  'src/content/terms-of-service/index.tsx',
  'src/content/privacyPolicy/index.tsx'
];

filesToClean.forEach(file => {
  const fullPath = path.join(process.cwd(), file);
  if (fs.existsSync(fullPath)) {
    let content = fs.readFileSync(fullPath, 'utf8');
    content = content.replace(/import\s+LanguageSwitcher\s+from\s+['"].*LanguageSwitcher['"];
?/g, '');
    content = content.replace(/<LanguageSwitcher\s*\/?>(?:<\/LanguageSwitcher>)?/g, '');
    content = content.replace(/<LanguageSwitcher\s*[^>]*>(?:<\/LanguageSwitcher>)?/g, '');
    fs.writeFileSync(fullPath, content);
  }
});
console.log('LanguageSwitcher removed.');
