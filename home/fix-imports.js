const fs = require('fs');
const path = require('path');

function processDir(dir) {
  if (!fs.existsSync(dir)) return;
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      processDir(fullPath);
    } else if (fullPath.endsWith('.tsx') || fullPath.endsWith('.ts')) {
      let content = fs.readFileSync(fullPath, 'utf8');
      let changed = false;

      // Ensure 'use client' is present if the file uses hooks or MUI and doesn't already have it
      if ((content.includes('useNavigate') || content.includes('@mui') || content.includes('useState') || content.includes('useEffect')) && !content.includes('use client')) {
        content = '"use client";\n' + content;
        changed = true;
      }

      if (content.includes('react-router-dom')) {
        let newImports = [];
        if (content.includes('useNavigate')) {
           newImports.push("import { useRouter } from 'next/navigation';");
        }
        if (content.includes('Link as RouterLink') || content.includes('NavLink') || content.includes('Link')) {
           newImports.push("import Link from 'next/link';");
        }
        
        // simple replace of the import line
        content = content.replace(/import\s+\{([^}]+)\}\s+from\s+['"]react-router-dom['"];/g, newImports.join('\n'));
        
        // Replace useNavigate() with useRouter()
        content = content.replace(/useNavigate\(\)/g, 'useRouter()');
        // Replace navigate(..) with router.push(..)
        content = content.replace(/const navigate =/g, 'const router =');
        content = content.replace(/navigate\(/g, 'router.push(');

        // Replace RouterLink with Link
        content = content.replace(/<RouterLink/g, '<Link');
        content = content.replace(/<\/RouterLink>/g, '</Link>');
        content = content.replace(/component=\{RouterLink\}/g, 'component={Link}');
        content = content.replace(/to=\{([^}]+)\}/g, 'href={$1}');
        content = content.replace(/to=['"]([^'"]+)['"]/g, 'href="$1"');

        changed = true;
      }
      
      if (changed) {
        fs.writeFileSync(fullPath, content);
      }
    }
  }
}

processDir(path.join(process.cwd(), 'src'));
processDir(path.join(process.cwd(), 'components'));
console.log('Fixed imports successfully');
