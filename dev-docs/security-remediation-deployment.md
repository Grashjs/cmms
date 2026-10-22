# Guide de déploiement des correctifs de sécurité Atlas CMMS

Ce guide couvre la mise en service coordonnée des trois correctifs : inscription dans une entreprise sur invitation valide, distribution sécurisée des pièces jointes et contrôle des privilèges de gestion des utilisateurs et des rôles.

Il décrit les opérations à réaliser et les vérifications attendues. Les cases ci-dessous sont à compléter pendant le déploiement ; elles ne constituent pas une attestation de validation déjà effectuée.

## 1. Préparer la livraison

- Identifier la version à livrer, les instances API, les clients web et mobile, le fournisseur de stockage réellement utilisé (`MINIO` ou `GCP`), les proxys et les éventuels CDN.
- Construire les images API et web à partir du code corrigé, avec une référence de version identifiable, puis les sélectionner dans la configuration de déploiement effective. Le `docker-compose.yml` du dépôt utilise des images préconstruites et ne contient pas de section `build` pour ces services : modifier les sources ou redémarrer les conteneurs ne déploie pas les correctifs.
- Préparer la configuration complète de `nginx.conf`, notamment les emplacements `/api/files/content/` et `/storage/`. Si un autre proxy termine HTTPS, conserver ces protections sur le chemin réellement emprunté par les utilisateurs.
- Reconstruire les applications Android et iOS avec les dépendances verrouillées, dont `expo-clipboard`. Installer les pods iOS selon le processus habituel, puis prévoir la distribution de la version mobile corrigée.
- Préparer une sauvegarde restaurable de PostgreSQL et du stockage, ainsi qu'une copie des configurations et métadonnées du bucket. Conserver les versions historiques des objets.
- Prévoir une fenêtre de maintenance et informer les utilisateurs du renouvellement des invitations en attente. Les changements de droits pourront également imposer une reconnexion aux comptes concernés.
- Exécuter les vérifications fonctionnelles de la section 9 sur une instance isolée avant la bascule. Utiliser deux entreprises fictives, des comptes dédiés et un stockage de test.

### Configuration à vérifier

| Paramètre | Valeur ou comportement attendu |
| --- | --- |
| `PUBLIC_API_URL` | URL publique de l'API, avec le suffixe `/api` si le proxy l'utilise. Les nouveaux liens de fichiers sont construits à partir de cette valeur. |
| `PUBLIC_FRONT_URL` | URL publique du client web utilisée pour les liens d'invitation. |
| `INVITATION_VIA_EMAIL` | Valeur cohérente entre l'API et le web. Pour l'inscription, cette option commande l'envoi d'emails ; une invitation valide reste obligatoire dans les deux modes pour rejoindre une entreprise existante. |
| `ENABLE_EMAIL_NOTIFICATIONS` et configuration d'envoi | À vérifier si les invitations doivent partir par email. La livraison manuelle par copie ou partage du lien reste disponible. |
| `STORAGE_TYPE` et paramètres du fournisseur | Fournisseur, bucket et identifiants correspondant au stockage cible. Le compte utilisé par l'API doit conserver ses accès après sécurisation du bucket. |
| `STORAGE_DELIVERY_SIGNING_KEY` | Clé dédiée facultative, aléatoire, d'au moins 32 caractères et identique sur toutes les instances API. Sans clé dédiée, le code utilise `JWT_SECRET_KEY`, qui doit respecter la même longueur minimale. Transmettre explicitement cette variable au conteneur si elle est utilisée : elle n'est pas déclarée dans le Compose actuel. |

La clé de distribution des pièces jointes reste côté serveur. Sa rotation invalide les liens API déjà émis. Elle est distincte des identifiants du fournisseur qui ont signé les anciens liens directs. `PUBLIC_MINIO_ENDPOINT` n'est plus utilisé pour générer les nouveaux liens de fichiers.

## 2. Passer en maintenance et suspendre les écritures

1. Restreindre l'accès à l'application pendant la bascule et arrêter les anciennes instances API susceptibles de traiter des inscriptions, invitations, modifications de droits ou dépôts de fichiers.
2. Suspendre les autres producteurs de fichiers : intégrations, remplacements, exports et traitements en arrière-plan. Une simple fermeture de l'interface web ne suspend pas ces opérations.
3. Finaliser les sauvegardes cohérentes avant migration. Confirmer le bucket, la base et le périmètre concernés.
4. Maintenir la suspension pendant la mise à jour des métadonnées et jusqu'à la validation des nouvelles instances. Les accès nécessaires à l'opérateur et aux contrôles restent restreints.

## 3. Fermer les anciens accès au stockage

### MinIO

- Garder les ports `9000` et `9001` dans le réseau privé nécessaire à l'API et au proxy ; vérifier qu'aucune autre publication ne les expose sur Internet.
- Retirer les politiques permettant une lecture anonyme des buckets ou objets concernés.
- Bloquer les chemins directs qui contourneraient l'API ou le proxy corrigé, y compris ceux qui permettent d'atteindre d'anciennes versions d'objets.

### Google Cloud Storage, si utilisé

- Retirer les autorisations publiques `allUsers` et `allAuthenticatedUsers` des politiques IAM et des ACL applicables, y compris celles des objets et dossiers concernés.
- Activer la prévention de l'accès public tout en conservant les droits nécessaires au compte de service de l'API et à l'opération de maintenance.
- Vérifier la prise en compte effective de ces changements. La prévention de l'accès public ne bloque pas les URL signées. Voir la [documentation Google Cloud sur la prévention de l'accès public](https://docs.cloud.google.com/storage/docs/public-access-prevention).

### Liens signés déjà émis

Révoquer les identifiants ou clés ayant signé les anciens liens, ou effectuer leur rotation en retirant effectivement les anciens identifiants. Mettre à jour les secrets de l'API et des autres clients autorisés si nécessaire. La création d'une nouvelle clé en laissant l'ancienne active ne suffit pas.

Si la révocation immédiate est impossible, maintenir la restriction des accès et attendre l'expiration de tous les anciens liens avant réouverture. Inclure les liens vers des versions ou générations historiques et ceux comportant des paramètres comme `response-content-type` ou `response-content-disposition`. Pour GCS, un lien reste utilisable jusqu'à son expiration ou la rotation de sa clé de signature : voir la [documentation sur les URL signées](https://docs.cloud.google.com/storage/docs/access-control/signed-urls).

Changer uniquement les métadonnées des objets ou rendre le bucket privé ne garantit pas l'invalidation des anciens liens signés. Une maintenance de l'application seule ne bloque pas non plus un lien direct vers le fournisseur : contrôler cet accès séparément.

## 4. Mettre à jour les métadonnées des fichiers existants

L'outil `StorageMetadataMigration` est une commande Java autonome. Il ne se lance pas au démarrage de l'API et ne fait pas partie de Liquibase. Il modifie les consignes de distribution des fichiers : type neutre `application/octet-stream`, téléchargement avec un nom assaini, cache `private, no-store` et suppression de l'encodage de contenu déclaré. Les octets des fichiers sont conservés ; aucune suppression ni modification de la base de données n'est effectuée.

La commande parcourt tous les objets du bucket configuré, y compris ceux absents de la base. `STORAGE_MIGRATION_PREFIX` permet de limiter volontairement le périmètre. Pour traiter tout le bucket, laisser cette variable absente. Si le bucket est partagé avec d'autres applications, confirmer le périmètre avant application.

### Préparer la commande

Utiliser Java 17 et Maven avec les sources corrigées, sur un poste ou un conteneur de maintenance disposant d'un accès privé au stockage. L'image d'exécution API fournit un JRE et le JAR applicatif, pas l'environnement Maven nécessaire aux commandes suivantes. Ne pas exposer MinIO publiquement pour exécuter cet outil.

Depuis le dossier `api/`, préparer les classes et leurs dépendances ; cette compilation peut être faite avant la fenêtre de maintenance :

```sh
mvn -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target/storage-classpath.txt
```

Injecter les variables dans l'environnement du processus de maintenance par le mécanisme protégé habituel. La commande ne charge pas automatiquement le fichier `.env` de Compose ni la configuration Spring.

| Fournisseur | Variables nécessaires |
| --- | --- |
| MinIO | `STORAGE_TYPE=MINIO`, `MINIO_ENDPOINT`, `MINIO_BUCKET`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` |
| GCS | `STORAGE_TYPE=GCP`, `GCP_PROJECT_ID`, `GCP_BUCKET_NAME` et `GCP_JSON` ou `GCP_JSON_PATH`. Les identifiants par défaut Google sont également pris en charge par cet outil. |

Utiliser des adresses joignables depuis le processus de maintenance. Ne pas mettre les secrets dans les arguments de commande ni dans un compte rendu.

### Simuler, appliquer, puis contrôler

1. Lancer la simulation, qui ne réalise aucune écriture :

   ```sh
   java -cp "target/classes:$(cat target/storage-classpath.txt)" com.grash.storage.StorageMetadataMigration
   ```

2. Examiner les compteurs `scanned` et `would-update`, confirmer le périmètre et vérifier les contraintes de rétention, de verrouillage et de chiffrement du stockage. Confirmer que tous les producteurs de fichiers sont suspendus.
3. Appliquer :

   ```sh
   java -cp "target/classes:$(cat target/storage-classpath.txt)" com.grash.storage.StorageMetadataMigration --apply --uploads-paused
   ```

4. Relancer la première commande. Le résultat attendu est `would-update=0` pour le périmètre couvert. Comparer également les empreintes de contenu avant et après sur les fichiers de contrôle.

`--uploads-paused` confirme une condition opérationnelle ; cette option n'arrête aucun dépôt elle-même. Une erreur ou un conflit interrompt l'outil, mais des objets précédents peuvent déjà avoir été modifiés. Corriger la cause, puis relancer la simulation et l'application si nécessaire. Les relances ignorent les objets dont les métadonnées sont déjà conformes.

Avec MinIO, l'outil réécrit les métadonnées de la version courante par copie interne et vérifie l'ETag source. Il conserve les octets, les métadonnées personnalisées et les étiquettes. Il ne corrige pas les métadonnées des anciennes versions immuables : maintenir la protection de leurs accès directs et la révocation des liens. Avec GCS, il traite chaque génération et utilise des conditions sur la génération et la métagénération pour détecter les conflits.

## 5. Déployer l'API corrigée et vérifier les migrations

1. Garder les anciennes instances arrêtées et le trafic public suspendu.
2. Démarrer une instance API corrigée avec la configuration cible. Liquibase est activé par la configuration du dépôt et applique les migrations manquantes dans l'ordre de `api/src/main/resources/db/master.xml`. Vérifier qu'aucune configuration de l'environnement ne le désactive.
3. Contrôler le démarrage et l'application des deux migrations ci-dessous avant de démarrer les autres instances corrigées.

| Migration | Effet attendu |
| --- | --- |
| `2026_10_18_00000000001_secure_user_invitations.xml` | Ajoute les données de jeton et d'audit, conserve les anciennes invitations et les révoque. Leur entreprise et leur émetteur sont récupérés lorsque possible. |
| `2026_10_20_00000000001_invitation_authorization.xml` | Ajoute les empreintes des droits de l'émetteur et du rôle invité. Ne modifie aucun droit de compte ou de rôle. Les anciennes invitations sans empreintes sont refusées par le code corrigé. |

Sur une installation ayant déjà reçu le premier correctif, Liquibase ne doit pas rejouer une migration déjà enregistrée. Vérifier les identifiants `secure_user_invitations` et `2026_10_20_invitation_authorization` dans `DATABASECHANGELOG`, ainsi que l'absence d'erreur de migration. Ne pas supprimer leur historique pour forcer une réexécution.

En cas d'échec, conserver la maintenance et diagnostiquer avant de poursuivre. Éviter toute coexistence avec une ancienne API encore capable d'accepter des invitations ou de modifier des droits.

## 6. Déployer les clients et le proxy

1. Déployer le client web correspondant et charger la configuration Nginx corrigée. Vérifier sa syntaxe avant rechargement, puis contrôler les réponses réellement servies par le domaine public.
2. Vérifier que les nouveaux liens de fichiers passent par `/api/files/content/` lorsque l'API publique utilise `/api`. Le serveur fournit aussi ces nouveaux liens aux fichiers existants lorsque les informations sont rechargées.
3. Vérifier que l'ancien emplacement `/storage/` force le téléchargement, sans exception pour les PDF, avec les protections `sandbox` et `nosniff`. Un contrôle réussi sur ce chemin ne prouve pas qu'un accès direct au fournisseur est sécurisé.
4. Si le web et l'API utilisent des origines différentes, autoriser l'origine de l'API dans la directive `img-src` de la politique du web.
5. Garder la distribution des pièces jointes hors cache. Purger les caches proxy/CDN maîtrisés et tenir compte de la durée de vie des réponses déjà distribuées par le fournisseur. Une modification d'ACL ne rappelle pas les copies déjà mises en cache : voir les [règles de cache de Google Cloud Storage](https://docs.cloud.google.com/storage/docs/metadata#cache-control).
6. Masquer les liens signés dans les journaux d'accès des proxys et outils de supervision supplémentaires. Ne pas enregistrer les jetons d'invitation, les liens de fichiers ou les clés dans le bilan de déploiement.
7. Distribuer la version mobile reconstruite après sa validation sur appareils. Faire recharger le web et les listes de fichiers ; les clients doivent récupérer les nouveaux liens au lieu de réutiliser des URL du fournisseur.

## 7. Renouveler les invitations en attente

Les comptes déjà inscrits sont conservés. Les destinataires qui doivent encore rejoindre l'entreprise doivent recevoir une nouvelle invitation émise par un membre actuellement autorisé, avec le rôle approprié.

Deux populations sont à examiner après les migrations :

- Les anciennes invitations fondées sur `?email=...&role=...`, désormais révoquées. Elles ne figurent plus dans les invitations en attente de l'interface ; réinviter les destinataires toujours attendus depuis le formulaire normal.
- Les invitations créées après le premier correctif mais avant le troisième, encore en attente et sans empreintes d'autorisation. Elles doivent également être renouvelées.

Un opérateur autorisé peut identifier ces populations avec les requêtes en lecture seule suivantes :

```sql
-- Anciennes invitations sans jeton sécurisé.
SELECT id, email, company_id, invited_role_id, invited_by_id, revoked_at
FROM user_invitation
WHERE token_hash IS NULL;

-- Invitations encore en attente, émises sans empreintes d'autorisation.
SELECT id, email, company_id, invited_role_id, invited_by_id, expires_at
FROM user_invitation
WHERE consumed_at IS NULL
  AND revoked_at IS NULL
  AND (role_authorization_hash IS NULL OR issuer_authorization_hash IS NULL);
```

Vérifier l'entreprise avec un membre autorisé si elle n'a pas pu être récupérée. Vérifier aussi que la personne doit toujours être invitée et n'a pas déjà terminé son inscription. Ne pas réactiver une ancienne ligne, lui ajouter un secret ni reconstruire ses empreintes à partir des droits actuels.

Le nouveau lien est valable sept jours et utilisable une seule fois. Un renouvellement invalide le précédent. Sa création n'atteste pas de sa réception par email : contrôler la livraison lorsque ce canal est utilisé. Sur mobile, l'acceptation se fait dans le navigateur, puis la connexion dans l'application.

## 8. Contrôler les comptes et rôles existants

Après migration, un opérateur autorisé peut utiliser `scripts/audit-company-authorization.sql`, à conserver dans la livraison. Ce script est en lecture seule et aide à examiner les comptes privilégiés, les associations incohérentes, les invitations sans empreintes et les rôles personnalisés.

- Confirmer la légitimité des comptes `ADMIN`, `LIMITED_ADMIN` et des superadministrateurs.
- Examiner les propriétaires inattendus ou multiples et les associations incohérentes entre utilisateur, rôle et entreprise.
- Examiner les cinq ensembles de permissions des rôles personnalisés et leurs capacités effectives.
- Examiner, lorsque les journaux le permettent, les acceptations d'invitations émises par des comptes limités et les changements antérieurs de mots de passe de comptes privilégiés.

Le correctif ne répare pas automatiquement les droits déjà enregistrés et ne permet pas, à lui seul, de conclure à l'absence d'exploitation antérieure. Les résultats du script sont des éléments à examiner, pas une liste certaine de comptes compromis. Effectuer les corrections de droits par les parcours autorisés de l'application. Une intervention directe en base doit aussi prendre en charge la révocation des sessions et invitations concernées, car elle contourne les services applicatifs.

## 9. Vérifications avant remise en service

Effectuer les scénarios de refus, de concurrence, d'expiration forcée et de modification de droits sur l'instance isolée avec des données fictives. Les changements de configuration entre les deux modes d'invitation se font également sur cette instance. Sur la cible de production encore restreinte, vérifier ensuite la configuration effective, les migrations, les anciens accès et les parcours ordinaires avec des comptes de contrôle autorisés, sans modifier les droits ou données de vrais utilisateurs pour simuler une attaque.

### 9.1 Invitations et inscription

- [ ] Avec l'envoi d'emails désactivé, un membre autorisé crée une invitation, voit l'email, le lien et l'expiration, puis peut transmettre le lien manuellement. Aucun compte n'est créé au seul envoi de l'invitation.
- [ ] Dans une fenêtre privée, l'invitation impose l'email, l'entreprise et le rôle attendus. L'inscription puis la connexion réussissent dans cette entreprise.
- [ ] La réutilisation d'un lien consommé, l'ancien lien remplacé lors d'un renouvellement et les liens révoqués ou expirés sont refusés sans création de compte ni de session. Le nouveau lien issu du renouvellement reste utilisable.
- [ ] Deux acceptations simultanées ne créent qu'un compte et qu'une consommation de l'invitation.
- [ ] Une inscription visant une entreprise existante sans jeton valide, ou avec un email, un rôle ou une entreprise incohérents, est refusée, y compris via l'ancien format de requête.
- [ ] Répéter les parcours dans les deux modes `INVITATION_VIA_EMAIL`, avec email reçu lorsque l'envoi est activé et avec transmission manuelle lorsqu'il est désactivé ou non demandé.
- [ ] La création légitime d'une nouvelle entreprise reste possible selon la configuration existante. Une session déjà présente dans le navigateur ne rattache pas cette création à l'entreprise du demandeur.

### 9.2 Pièces jointes, stockage et anciens liens

- [ ] Déposer des fichiers de contrôle inoffensifs HTML, SVG, inconnus et images tronquées, y compris avec une extension ou un type déclaré trompeur (`.pdf`, `.png`). Vérifier les dépôts authentifiés et le portail public sans session.
- [ ] Ces fichiers sont téléchargés et aucun script de contrôle ne s'exécute dans le navigateur. Vérifier la réponse finale : `application/octet-stream`, `Content-Disposition: attachment`, `Cache-Control: private, no-store`, CSP `sandbox` et `X-Content-Type-Options: nosniff`.
- [ ] Répéter les contrôles via le proxy et directement via l'API. Les paramètres demandant un autre type ou mode d'affichage ne modifient pas la politique de réponse.
- [ ] Des images PNG, JPEG, GIF et WebP valides s'affichent avec leur type réellement vérifié, y compris si leur nom est trompeur. Vérifier aussi les logos et miniatures.
- [ ] Les vrais PDF, rapports générés et exports CSV se téléchargent, conservent leur contenu et s'ouvrent dans le lecteur prévu. Parcourir les fonctions métier de génération et d'export de bout en bout.
- [ ] Sur le stockage de test, la simulation ne change ni métadonnées ni octets. Après application, les octets sont identiques et une nouvelle simulation indique `would-update=0`.
- [ ] Sur le stockage cible, le périmètre prévu a été traité sans erreur non résolue. Les anciennes versions MinIO sont prises en compte dans le blocage des accès, même si leurs métadonnées n'ont pas été modifiées.
- [ ] Les anciens liens directs, signés et versionnés sont révoqués, expirés ou inaccessibles selon le plan retenu. Un ancien lien passant encore par `/storage/` ne peut servir qu'un téléchargement protégé. Vérifier séparément les URL natives du fournisseur.
- [ ] Si GCS est utilisé, vérifier avec un bucket isolé réel les IAM/ACL, les générations, la révocation des liens, la migration et les réponses HTTP ; contrôler ensuite la configuration effective du bucket cible.
- [ ] Vérifier les gros fichiers, les grandes images et la lecture audio avec déplacement dans la piste. Les images dépassant les limites d'inspection sont téléchargées et le point d'accès ne prend pas en charge les requêtes HTTP Range : confirmer la compatibilité des parcours utilisés.
- [ ] Aucun cache ou journal intermédiaire ne réintroduit une réponse ancienne dangereuse ou ne divulgue les nouveaux liens signés.

### 9.3 Utilisateurs, rôles et révocation des accès

Préparer, dans chacune des entreprises de test, un propriétaire, un administrateur, un gestionnaire limité de « People & Teams » et un technicien.

- [ ] Le gestionnaire peut inviter un rôle autorisé, mais ne peut inviter un administrateur ni attribuer un rôle dépassant ses droits. La liste web/mobile est filtrée et une requête directe interdite est refusée côté serveur.
- [ ] Il ne peut pas rétrograder un compte plus privilégié pour en prendre ensuite le contrôle, modifier son propre rôle ou augmenter indirectement ses droits en modifiant son rôle personnalisé.
- [ ] Les changements sensibles du propriétaire sont refusés à un autre compte, y compris administrateur. Les opérations utilisant un compte ou un rôle d'une autre entreprise, ou attribuant un rôle de superadministrateur, sont refusées.
- [ ] Les refus laissent les comptes et rôles inchangés. Les opérations autorisées sur des membres de droits compatibles, ainsi que les modifications ordinaires de profil, continuent de fonctionner.
- [ ] Modifier les droits de l'émetteur ou du rôle invité avant acceptation rend l'invitation inutilisable. Rétablir les anciens droits ne réactive pas ce lien ; un renouvellement par un membre autorisé fonctionne.
- [ ] Un changement de rôle, de permissions ou une désactivation invalide les anciennes sessions à la requête suivante, depuis plusieurs navigateurs et, si applicable, plusieurs instances API. Un compte encore autorisé peut se reconnecter.
- [ ] Si LDAP ou la gestion d'abonnement peuvent modifier les rôles ou l'activation, vérifier également ces parcours et la révocation des invitations concernées sur l'instance isolée.

### 9.4 Parcours de mot de passe existants

Ces contrôles vérifient le comportement livré. Le durcissement supplémentaire du changement personnel par l'API générale de profil reste un sujet distinct.

- [ ] Avec `INVITATION_VIA_EMAIL=true`, l'API générale de profil refuse `newPassword` avec HTTP 403, pour soi et pour autrui, y compris comme administrateur. Aucun autre champ du même patch ni aucune session ne sont modifiés par ce refus. Le web ne propose pas ce champ dans l'édition d'un membre.
- [ ] Avec `INVITATION_VIA_EMAIL=false`, le changement personnel historique par l'API générale reste possible sans ancien mot de passe. Pour autrui, seuls les administrateurs autorisés peuvent réinitialiser un compte de droits compatibles, dans la même entreprise et hors propriétaire. Vérifier les refus pour les autres acteurs.
- [ ] Dans les deux modes, le formulaire personnel web/mobile utilise `/auth/updatepwd` : ancien mot de passe incorrect refusé, ancien mot de passe correct accepté, nouvelles sessions utilisables et anciennes sessions refusées après changement.
- [ ] La récupération par lien sécurisé fonctionne et le lien ne peut pas être réutilisé après consommation.

### 9.5 Applications Android et iOS

- [ ] Valider sur appareils la version reconstruite : liste de rôles autorisés, création d'invitation, expiration affichée, absence de double envoi lors de pressions rapides et message de refus après retrait des droits.
- [ ] Vérifier la copie, le partage natif, leur annulation et leurs erreurs. Le résultat reste disponible pour réessayer tant que l'écran est ouvert.
- [ ] En quittant l'écran par fermeture ou retour, le lien disparaît. Une réponse tardive ne le réaffiche pas. Les liens ne sont pas enregistrés dans les journaux ou enregistrements de session ; vérifier l'exclusion de cet écran si Clarity est actif.
- [ ] Accepter le lien dans le navigateur puis se connecter dans l'application. Vérifier aussi le renouvellement, le refus des anciens liens, les deux choix de livraison et les parcours de récupération/reconnexion.
- [ ] Ouvrir des pièces jointes depuis les équipements, emplacements, pièces, interventions et commentaires : affichage des images validées, téléchargement externe des documents, rapports générés et message d'erreur pour un lien invalide ou expiré.

## 10. Autoriser la réouverture

- [ ] Toutes les instances API utilisent la version corrigée ; les deux migrations sont enregistrées et aucun ancien serveur ne traite encore les opérations concernées.
- [ ] Le web et le proxy corrigés sont actifs. La version mobile correspondante est validée et sa distribution est prise en charge.
- [ ] Le stockage, les anciens liens et les caches ont été traités ; aucune erreur de migration de métadonnées ne reste sans explication.
- [ ] Les invitations concernées ont été identifiées et leur renouvellement est organisé, sans réactivation des anciennes lignes.
- [ ] Les vérifications applicables ont un résultat consigné. Tout refus contournable, exécution de contenu ou échec de migration maintient la restriction du service concerné jusqu'à correction.
- [ ] La revue des comptes et rôles existants est prise en charge, avec traitement des anomalies identifiées.

Rétablir ensuite les accès et reprendre les dépôts et traitements suspendus. Consigner la version livrée, les migrations appliquées, les compteurs de traitement et les résultats des vérifications, sans secrets ni liens signés. Surveiller les erreurs d'inscription, les refus inattendus, l'ouverture des pièces jointes et les reconnexions pendant la reprise.

En cas d'incident, maintenir ou rétablir la restriction des opérations affectées. Ne pas remettre une ancienne version vulnérable à disposition du public comme simple retour arrière ; préparer la reprise avec les sauvegardes et vérifier les protections avant réouverture.
