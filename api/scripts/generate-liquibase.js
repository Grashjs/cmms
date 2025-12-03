const fs = require('fs');
const path = require('path');

// Fonction pour parser les arguments nommés
function parseArgs() {
    const args = process.argv.slice(2);
    const parsed = {};

    for (const arg of args) {
        if (arg.startsWith('--')) {
            const [key, value] = arg.split('=');
            const cleanKey = key.replace('--', '');
            const cleanValue = value ? value.replace(/^["']|["']$/g, '') : true;
            parsed[cleanKey] = cleanValue;
        }
    }

    return parsed;
}

// Fonction pour afficher l'aide
function showHelp() {
    console.log('🚀 Générateur de changelog Liquibase\n');
    console.log('Usage:');
    console.log('  node scripts/generate-liquibase.js --author="Nom Auteur" --name="nom_migration"\n');
    console.log('Options:');
    console.log('  --author    Nom de l\'auteur du changeset (obligatoire)');
    console.log('  --name      Nom de la migration (obligatoire)');
    console.log('  --help      Affiche cette aide\n');
    console.log('Exemple:');
    console.log('  node scripts/generate-liquibase.js --author="Author" --name="changelog_name"');
}

// Fonction pour formater la date au format YYYY_MM_DD_HHMM
function formatDate() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');

    return `${year}_${month}_${day}`;
}

// Fonction pour générer le timestamp Unix (en secondes comme Laravel)
function generateTimestamp() {
    return Math.floor(Date.now() / 1000);
}

// Fonction pour créer le contenu XML
function createXmlContent(timestamp, author) {
    return `<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd">

    <changeSet id="${timestamp}-1" author="${author}">
    </changeSet>
</databaseChangeLog>
`;
}

// Fonction pour ajouter l'include dans master.xml
function addToMasterXml(fileName) {
    const masterXmlPath = path.join(process.cwd(), 'src', 'main', 'resources', 'db', 'master.xml');

    // Vérifier si master.xml existe
    if (!fs.existsSync(masterXmlPath)) {
        console.warn(`⚠️  Attention: Le fichier master.xml n'existe pas à ${masterXmlPath}`);
        return false;
    }

    try {
        // Lire le contenu de master.xml
        let masterContent = fs.readFileSync(masterXmlPath, 'utf8');

        // Vérifier si le fichier est déjà référencé
        if (masterContent.includes(`changelog/${fileName}`)) {
            console.warn(`⚠️  Le fichier ${fileName} est déjà référencé dans master.xml`);
            return false;
        }

        // Créer la nouvelle ligne d'include
        const newInclude = `    <include file="changelog/${fileName}"
             relativeToChangelogFile="true"/>`;

        // Trouver la position avant la balise fermante </databaseChangeLog>
        const closingTag = '</databaseChangeLog>';
        const closingTagIndex = masterContent.lastIndexOf(closingTag);

        if (closingTagIndex === -1) {
            console.error('❌ Impossible de trouver la balise </databaseChangeLog> dans master.xml');
            return false;
        }

        // Insérer la nouvelle ligne avant la balise fermante
        const beforeClosing = masterContent.substring(0, closingTagIndex);
        const afterClosing = masterContent.substring(closingTagIndex);

        const updatedContent = beforeClosing + newInclude + '\n' + afterClosing;

        // Écrire le fichier modifié
        fs.writeFileSync(masterXmlPath, updatedContent, 'utf8');

        console.log(`📝 Référence ajoutée dans master.xml`);
        return true;

    } catch (error) {
        console.error(`❌ Erreur lors de la mise à jour de master.xml: ${error.message}`);
        return false;
    }
}

// Fonction principale
function main() {
    const args = parseArgs();

    // Vérifier si l'aide est demandée
    if (args.help) {
        showHelp();
        return;
    }

    // Vérifier les arguments obligatoires
    if (!args.author || !args.name) {
        console.error('❌ Erreur: Les arguments --author et --name sont obligatoires\n');
        showHelp();
        process.exit(1);
    }

    const author = args.author;
    const name = args.name;

    // Générer le timestamp et formater la date
    const timestamp = generateTimestamp();
    const dateFormatted = formatDate();

    // Créer le nom du fichier
    const fileName = `${dateFormatted}_${timestamp}_${name}.xml`;

    // Créer le contenu XML
    const xmlContent = createXmlContent(timestamp, author);

    // Créer le dossier db/changelog s'il n'existe pas
    const changelogDir = path.join(process.cwd(), 'src', 'main', 'resources', 'db', 'changelog');
    if (!fs.existsSync(changelogDir)) {
        fs.mkdirSync(changelogDir, { recursive: true });
        console.log(`📁 Dossier créé: ${changelogDir}`);
    }

    // Chemin complet du fichier
    const filePath = path.join(changelogDir, fileName);

    // Vérifier si le fichier existe déjà
    if (fs.existsSync(filePath)) {
        console.error(`❌ Le fichier ${fileName} existe déjà !`);
        process.exit(1);
    }

    // Écrire le fichier changelog
    try {
        fs.writeFileSync(filePath, xmlContent, 'utf8');
        console.log(`✅ Changelog créé avec succès !`);
        console.log(`📄 Fichier: ${fileName}`);
        console.log(`📍 Chemin: ${filePath}`);
        console.log(`👤 Auteur: ${author}`);
        console.log(`🆔 ID: ${timestamp}-1`);

        // Ajouter la référence dans master.xml
        addToMasterXml(fileName);

    } catch (error) {
        console.error(`❌ Erreur lors de la création du fichier: ${error.message}`);
        process.exit(1);
    }
}

// Exécuter le script
if (require.main === module) {
    main();
}

module.exports = { formatDate, generateTimestamp, createXmlContent, parseArgs, addToMasterXml };