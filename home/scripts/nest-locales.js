const flat = require('flat');
const fs = require('fs');
const path = require('path');
const { Project } = require('ts-morph');

const messagesDir = path.join(__dirname, '../src/i18n/translations');
const project = new Project();

const files = fs.readdirSync(messagesDir).filter(f => f.endsWith('.ts'));

files.forEach(file => {
    const filePath = path.join(messagesDir, file);
    const sourceFile = project.addSourceFileAtPath(filePath);

    const varDecl = sourceFile.getVariableDeclarations()[0];
    if (!varDecl) { console.log(`⚠️  Skipped ${file}`); return; }

    const varName = varDecl.getName();
    const initializer = varDecl.getInitializer();
    if (!initializer) { console.log(`⚠️  Skipped ${file}`); return; }

    const flatObj = {};
    initializer.getProperties().forEach(prop => {
        const key = prop.getNameNode().getLiteralText?.() ?? prop.getName();
        const value = prop.getInitializer()?.getLiteralText?.() ?? prop.getInitializer()?.getText();
        flatObj[key] = value;
    });

    // Find namespaces that conflict (have both a plain key and dot-notation children)
    const namespaces = new Set(
        Object.keys(flatObj)
            .filter(k => k.includes('.'))
            .map(k => k.split('.')[0])
    );

    const resolvedObj = {};
    Object.entries(flatObj).forEach(([key, value]) => {
        if (key.includes('.')) {
            const namespace = key.split('.')[0];
            if (flatObj[namespace] !== undefined) {
                // Conflict: rename nested key namespace to namespace_1
                const newKey = key.replace(`${namespace}.`, `${namespace}_1.`);
                console.log(`⚠️  Conflict in ${file}: "${key}" → "${newKey}"`);
                resolvedObj[newKey] = value;
            } else {
                resolvedObj[key] = value;
            }
        } else {
            resolvedObj[key] = value;
        }
    });

    const nested = flat.unflatten(resolvedObj);

    const newContent = `const ${varName} = ${JSON.stringify(nested, null, 2)};\n\nexport default ${varName};\n`;
    fs.writeFileSync(filePath, newContent);
    console.log(`✅ Converted ${file}`);

    project.removeSourceFile(sourceFile);
});