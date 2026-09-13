<div align="center">

# 🟦 HeroTab

**Le tab list unifié, fluide et 100 % personnalisable du réseau HeroCraft.**

[![Version](https://img.shields.io/badge/version-2.0.2-00b4ff?style=flat-square)](#-nouveautés-de-la-v202)
[![Velocity](https://img.shields.io/badge/Velocity-3.3.0%2B-blueviolet?style=flat-square)](#-compatibilité)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)](#-compatibilité)
[![License](https://img.shields.io/badge/license-MIT-success?style=flat-square)](LICENSE)
[![Network](https://img.shields.io/badge/HeroCraft-Officiel-ff69b4?style=flat-square)](https://github.com/herocraftlol)

Plugin **Velocity** qui remplace la liste vanilla des joueurs par une expérience cohérente sur **tout le réseau HeroCraft** : un seul onglet « joueurs » pour tous les sous-serveurs Paper, avec header/footer animés, format de ligne et tri configurables, lecture directe MySQL pour GradePlugin et FactionPlugin, et un mode `safe` qui préserve les skins (SkinRestorer, Bedrock, comptes premium) sans concession sur le rendu.

</div>

---

## 🤔 Pourquoi HeroTab ?

Par défaut, l'onglet « joueurs » de Minecraft n'est pas pensé pour les réseaux multi-serveurs : chaque backend Paper affiche son propre sous-ensemble de joueurs, sans les grades, sans la faction, sans aucune animation. Pour un réseau comme **HeroCraft**, qui regroupe Factions, BedWars, HikaBrain, HungerGames et d'autres sous-serveurs derrière un proxy Velocity, ça donne à l'arrivée :

- 😕 Un joueur Factions qui rejoint HikaBrain ne voit plus son tag de faction.
- 😕 Les joueurs de BedWars et HungerGames se « mélangent » en vrac, sans distinction visuelle du serveur.
- 😕 Les grades achetés sur GradePlugin (VIP, Premium…) n'apparaissent jamais dans le tab des autres serveurs.
- 😕 Aucun header/footer animé, aucune info réseau, aucun branding central.
- 😕 La couleur de serveur n'est pas appliquée — tout le réseau ressemble au même sous-serveur générique.

**HeroTab corrige tout ça côté proxy** : un seul onglet « joueurs » cohérent pour tout le monde, configurable depuis un simple `config.yml`, sans rien installer côté Paper.

---

## ✨ Fonctionnalités

### 🎨 Tab list personnalisable
- **Header & footer multi-lignes** avec substitution complète de placeholders (`%online%`, `%server%`, `%network_address%`, `%player%`…).
- **Frames animées** : la syntaxe `frame1||frame2||frame3` fait défiler automatiquement le texte à la vitesse `animation-interval-ticks`.
- **Format joueur** : un patron unique (`player-format`) appliqué à toutes les entrées du tab — serveur, ping, grade, faction, préfixe…
- **Thème centralisé** : changer `theme-primary` / `theme-secondary` repeint toute la décoration d'un coup.
- **Codes `&` classiques** (`&b`, `&l`, `&7`…) **+ hex `&#RRGGBB`** **+ MiniMessage** (`<gradient:#00c6ff:#0072ff>…</gradient>`) — ne mélangez pas `&` et `<tags>` sur la même ligne.
- **Regroupement de sous-serveurs** : `bedwars1`, `bedwars2`, `bedwars3` peuvent tous apparaître sous le label « BedWars » via `server-groups`.
- **Couleur individuelle par serveur** : `server-colors` permet de coloriser `%server_color%` différemment pour chaque backend, sans casser le thème global (absent = retombe sur `theme-primary`).

### 📊 Ordre des joueurs (configurable)
- `ALPHABETICAL` — tri alphabétique des pseudos.
- `PING` — tri par ping croissant (les joueurs les plus réactifs en premier).
- `SERVER` — regroupés par sous-serveur (ordre alphabétique des serveurs).
- **`SERVER_SELF_FIRST`** *(par défaut)* — les joueurs de **ton** serveur actuel en premier, séparateur décoratif, puis le reste du réseau.
- `NONE` — ordre de connexion Velocity, pas de tri.

### 🛡️ Mode `safe` par défaut — préservation des skins
Le protocole Minecraft n'a pas de notion de position sur une entrée déjà créée : pour vraiment réordonner le tab, il faut **retirer puis recréer** les entrées — ce qui casse les skins externes (SkinRestorer, Bedrock, comptes premium classiques) sur de nombreuses installations.

HeroTab gère ça nativement :

- **`reorder-mode: safe`** *(par défaut)* — on ne touche **jamais** à l'identité d'une entrée déjà présente. Seuls le texte et le ping sont rafraîchis → skins garantis intacts. Les options `sort-mode` et `group-spacer` n'ont alors pas d'effet visuel, mais la stabilité est maximale. Quand un joueur n'est plus en ligne, son entrée est retirée ; quand un joueur d'un autre serveur manque, il est ajouté — comme ça la visibilité réseau reste complète, sans jamais toucher aux skins des joueurs déjà affichés.
- **`reorder-mode: experimental`** — active le vrai tri visuel (sort-mode, group-spacer) pour les setups où les skins ne posent pas problème. Le plugin ne retire/ré-ajoute les entrées **que quand l'ordre a réellement changé** (arrivée, départ, changement de serveur), pas à chaque cycle, pour laisser le temps aux textures externes de se stabiliser. Un cache `knownGoodProfiles` mémorise en outre le profil complet (avec skin) de chaque joueur vu, pour ne rien perdre visuellement même quand une entrée doit être recréée.
- **Séparateur de groupe configurable** : `group-spacer-text` permet de définir le motif décoratif inséré entre chaque bloc de serveur en `SERVER` / `SERVER_SELF_FIRST`.

### 🔗 Intégrations MySQL natives (lecture seule)
- **GradePlugin** : lecture directe de `grades_db` (tables `player_grades` + `grades`), rafraîchissement périodique configurable. Placeholders `%grade%`, `%grade_prefix%`, `%grade_color%`, `%grade_suffix%` automatiquement remplis, avec `ORDER BY priority DESC` pour prendre le grade le plus prioritaire.
- **FactionPlugin** : lecture de la table `faction_tab_sync`. Le tag de faction (`%faction_tag%`) **n'est visible que si le viewer ET la cible sont sur le serveur Factions** — invisible partout ailleurs sur le réseau (pas de spoil inter-serveurs).
- **Driver MySQL embarqué** : `mysql-connector-j` est *shadé* dans le JAR final. Un chargeur dédié (`JdbcDriverLoader`) contourne le problème d'isolation de classloaders de Velocity pour que les connexions fonctionnent depuis une tâche planifiée (le `ServiceLoader` standard renvoie sinon `No suitable driver found`).
- **Refresh immédiat à la connexion** : `ServerConnectedEvent` force une relecture MySQL + un `updateAll()` du tab — fini les 15 s d'attente après un `/f join` ou un changement de grade.

### 🧩 Autres commodités
- **Commande admin** : `/herotab reload` (alias `/htab`) pour recharger la configuration sans redémarrer le proxy. Annule et recrée proprement les tâches planifiées (update + intégrations MySQL), aucune fuite, aucune tâche orpheline.
- **Permission unique** : tout est sous `herotab.admin`.
- **`update-interval-ticks`** configurable, converti automatiquement côté Velocity en millisecondes (20 ticks = 1 s, minimum 200 ms).
- **Logs au démarrage** : nom du plugin, version, nombre de groupes de serveurs, état des intégrations MySQL activées.
- **Replay propre des animations** : l'index d'animation est dérivé de l'horloge système — pas de sauvegarde d'état nécessaire, redémarrer le proxy repart d'une animation cohérente.

---

## 🆕 Nouveautés de la v2.0.2

Cette version consolide le code source et harmonise une dernière fois toutes les surfaces de version et de description. C'est une mise à jour **« drop-in »** : remplacez le JAR et `/herotab reload`, aucune migration de config n'est nécessaire.

- 🆙 **Version bumpée à 2.0.2** dans `pom.xml`, l'annotation `@Plugin` et le `velocity-plugin.json` généré — toutes les surfaces de version (JAR, panneau d'admin Velocity, logs) restent synchronisées.
- 📝 **Description enrichie dans le code source** : la balise `<description>` de `pom.xml` et le champ `description` de `@Plugin` portent désormais la même description détaillée (tab unifié, animations, intégration MySQL GradePlugin & FactionPlugin, mode safe pour les skins, tag de faction limité au serveur Factions, JAR shaded avec dépendances embarquées, compatibilité Velocity 3.3.0+/Java 17+). Un seul coup d'œil dans le listing Velocity ou le panneau d'admin suffit désormais à comprendre ce que fait le plugin.
- 🏷️ **Descriptions cohérentes d'un fichier à l'autre** : fini les incohérences entre ce qu'annonce `pom.xml`, ce que déclare `@Plugin`, ce qu'affiche `velocity-plugin.json` et ce qui est documenté dans ce README.
- 📦 **JAR shaded reconstruit** : `mysql-connector-j` et `snakeyaml` (relocalisé sous `com.herocraft.herotab.libs.snakeyaml` pour éviter toute collision avec d'autres plugins) sont toujours embarqués. Aucune dépendance externe à gérer côté serveur.
- 🔁 **100 % rétrocompatible** avec la v2.0.1 et toutes les versions précédentes : la structure du `config.yml` reste inchangée, le format des placeholders est identique, les tâches planifiées utilisent la même signature. Aucune migration n'est nécessaire.

---

## 📦 Installation

1. Téléchargez la dernière version `herotab-X.Y.Z.jar` depuis la page **[Releases](../../releases)**.
2. Déposez le JAR dans le dossier `plugins/` de votre proxy **Velocity**.
3. (Re)démarrez le proxy — un `plugins/herotab/config.yml` est créé automatiquement.
4. Éditez la configuration à votre goût, puis faites `/herotab reload` (permission `herotab.admin`).

> **Compatibilité :** Velocity **3.3.0+** · Java **17+** · aucun plugin Paper requis (les intégrations Grade/Faction se font en lecture directe MySQL).

---

## 🛠️ Compilation depuis les sources

```bash
unzip HeroTab-2.0.2-source.zip
cd HeroTab/herotab
mvn clean package
```

Le JAR final sera dans `herotab/target/herotab-2.0.2.jar`.

---

## 📜 Licence

Distribué sous licence MIT — voir le fichier [LICENSE](LICENSE).

_Ce message a été préparé par un agent OpenHands au nom de l'équipe HeroCraft._
