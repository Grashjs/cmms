const fs = require('fs');
const path = require('path');

// Template replacement function
function processTemplate(template, params) {
    let result = template;

    // Replace {name}[-C] with PascalCase
    result = result.replace(/\{name\}\[-C\]/g, toPascalCase(params.name));

    // Replace {name}[-c] with camelCase
    result = result.replace(/\{name\}\[-c\]/g, toCamelCase(params.name));

    // Replace {name}[-d] with kebab-case
    result = result.replace(/\{name\}\[-d\]/g, toKebabCase(params.name));

    // Replace {name}[-s] with snake_case
    result = result.replace(/\{name\}\[-s\]/g, toSnakeCase(params.name));

    // Replace date placeholders
    const now = new Date();
    result = result.replace(/\{year\}/g, now.getFullYear());
    result = result.replace(/\{month\}/g, String(now.getMonth() + 1).padStart(2, '0'));
    result = result.replace(/\{day\}/g, String(now.getDate()).padStart(2, '0'));

    return result;
}

// Case conversion functions
function toPascalCase(str) {
    return str
        .replace(/[_-]([a-z])/g, (_, char) => char.toUpperCase())
        .replace(/^[a-z]/, char => char.toUpperCase());
}

function toCamelCase(str) {
    const pascal = toPascalCase(str);
    return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

function toKebabCase(str) {
    return str
        .replace(/([a-z])([A-Z])/g, '$1-$2')
        .replace(/[_\s]+/g, '-')
        .toLowerCase();
}

function toSnakeCase(str) {
    return str
        .replace(/([a-z])([A-Z])/g, '$1_$2')
        .replace(/[-\s]+/g, '_')
        .toLowerCase();
}

// Main generator function
function generateFiles(config, params) {
    const { name, description, addFile, globalBasePath } = config;
    const templatesDir = path.join(__dirname, '..', 'templates', name);
    const outputBaseDir = path.join(__dirname, '..', 'src', 'main');

    console.log(`Generating files for: ${description}`);
    console.log(`Entity name: ${params.name}\n`);

    addFile.forEach(file => {
        try {
            // Read template file
            const templatePath = path.join(templatesDir, file.fileTemplatePath);
            const templateContent = fs.readFileSync(templatePath, 'utf-8');

            // Process template
            const processedContent = processTemplate(templateContent, params);

            // Process output filename and path
            const processedFileName = processTemplate(file.name, params);
            const processedFilePath = processTemplate(file.path, params);

            // Create full output path
            const fullOutputDir = path.join(outputBaseDir, processedFilePath);
            const fullOutputPath = path.join(fullOutputDir, processedFileName);

            // Create directories if they don't exist
            fs.mkdirSync(fullOutputDir, { recursive: true });

            // Write file
            fs.writeFileSync(fullOutputPath, processedContent, 'utf-8');

            console.log(`✓ Created: ${fullOutputPath}`);
        } catch (error) {
            console.error(`✗ Error creating ${file.name}:`, error.message);
        }
    });

    console.log('\nGeneration complete!');
}

// CLI interface
function main() {
    const args = process.argv.slice(2);

    if (args.length < 2) {
        console.log('Usage: node generate-template.js <template-name> <entity-name>');
        console.log('Example: node generate-template.js "New Entity" Patient');
        process.exit(1);
    }

    const templateName = args[0];
    const entityName = args[1];

    // Load config
    const configPath = path.join(__dirname, '..', 'templates', templateName, 'main.json');

    if (!fs.existsSync(configPath)) {
        console.error(`Error: Template "${templateName}" not found at ${configPath}`);
        process.exit(1);
    }

    const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));

    // Generate files
    generateFiles(config, { name: entityName });
}

// Run if called directly
if (require.main === module) {
    main();
}

// Export for use as module
module.exports = { generateFiles, processTemplate };