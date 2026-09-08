<div align="center">

# 🟦 HeroTab

**Le tab list unifié, fluide et 100 % personnalisable du réseau HeroCraft — avec tag de faction injecté dans le chat, partout sur le réseau.**

[![Version](https://img.shields.io/badge/version-2.0.0-00b4ff?style=flat-square)](#-nouveautés-de-la-v200)
[![Velocity](https://img.shields.io/badge/Velocity-3.3.0%2B-blueviolet?style=flat-square)](#-compatibilité)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)](#-compatibilité)
[![License](https://img.shields.io/badge/license-MIT-success?style=flat-square)](LICENSE)
[![Network](https://img.shields.io/badge/HeroCraft-Officiel-ff69b4?style=flat-square)](https://github.com/herocraftlol)

Plugin **Velocity** qui remplace la liste vanilla des joueurs par une expérience cohérente sur **tout le réseau HeroCraft** : un seul onglet « joueurs » pour tous les sous-serveurs Paper, avec header/footer animés, format de ligne et tri configurables, **tag de faction injecté automatiquement dans le chat** (texte brut, compatible avec tous les anti-triche), lecture directe MySQL pour GradePlugin et FactionPlugin, et un mode `safe` qui préserve les skins (SkinRestorer, Bedrock, comptes premium) sans concession sur le rendu.

</div>

---

## 🤔 Pourquoi HeroTab ?

Par défaut, l'onglet « joueurs » de Minecraft n'est pas pensé pour les réseaux multi-serveurs : chaque backend Paper affiche son propre sous-ensemble de joueurs, sans les grades, sans la faction, sans aucune animation. Pour un réseau comme **HeroCraft**, qui regroupe Factions, BedWars, HikaBrain, HungerGames et d'autres sous-serveurs derrière un proxy Velocity, ça donne à l'arrivée :

- 😕 Un joueur Factions qui rejoint HikaBrain ne voit plus son tag de faction.
- 😕 Les joueurs de BedWars et HungerGames se « mélangent » en vrac, sans distinction visuelle du serveur.
- 😕 Les grades achetés sur GradePlugin (VIP, Premium…) n'apparaissent jamais dans le tab des autres serveurs.
- 😕 Aucun header/footer animé, aucune info réseau, aucun branding central.
- 😕 L'identité de faction d'un joueur disparaît dès qu'il parle ailleurs que sur Factions.

**HeroTab corrige tout ça côté proxy** : un seul onglet « joueurs » cohérent pour tout le monde, configurable depuis un simple `config.yml`, sans rien installer côté Paper. Le tag de faction suit maintenant le joueur jusque dans le chat, sur **tout** le réseau.

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

- **`reorder-mode: safe`** *(par défaut)* — on ne touche **jamais** à l'identité d'une entrée déjà présente. Seuls le texte et le ping sont rafraîchis → skins garantis intacts. Les options `sort-mode` et `group-spacer` n'ont alors pas d'effet visuel, mais la stabilité est maximale.
- **`reorder-mode: experimental`** — active le vrai tri visuel (sort-mode, group-spacer) pour les setups où les skins ne posent pas problème. Le plugin ne retire/ré-ajoute les entrées **que quand l'ordre a réellement changé** (arrivée, départ, changement de serveur), pas à chaque cycle, pour laisser le temps aux textures externes de se stabiliser.
- **Séparateur de groupe configurable** : `group-spacer-text` permet de définir le motif décoratif inséré entre chaque bloc de serveur en `SERVER` / `SERVER_SELF_FIRST`.

### 🔗 Intégrations MySQL natives (lecture seule)
- **GradePlugin** : lecture directe de `grades_db` (tables `player_grades` + `grades`), rafraîchissement périodique configurable. Placeholders `%grade%`, `%grade_prefix%`, `%grade_color%`, `%grade_suffix%` automatiquement remplis, avec `ORDER BY priority DESC` pour prendre le grade le plus prioritaire.
- **FactionPlugin** : lecture de la table `faction_tab_sync`. Le tag de faction (`%faction_tag%`) **n'est visible que si le viewer ET la cible sont sur le serveur Factions** — invisible partout ailleurs sur le réseau (pas de spoil inter-serveurs).
- **Driver MySQL embarqué** : `mysql-connector-j` est *shadé* dans le JAR final. Un chargeur dédié (`JdbcDriverLoader`) contourne le problème d'isolation de classloaders de Velocity pour que les connexions fonctionnent depuis une tâche planifiée (le `ServiceLoader` standard renvoie sinon `No suitable driver found`).
- **Refresh immédiat à la connexion** : `ServerConnectedEvent` force une relecture MySQL + un `updateAll()` du tab — fini les 15 s d'attente après un `/f join` ou un changement de grade.

### 💬 Tag de faction dans le chat
Au-delà du tab, HeroTab peut **préfixer automatiquement** chaque message de chat des joueurs membres d'une faction, **sur tout le réseau** — peu importe le sous-serveur sur lequel ils se trouvent. Fini le tag affiché seulement sur Factions : un joueur qui va jouer une partie de BedWars garde son identité de faction quand il parle.

- Active / désactive globalement via `chat-faction-tag-enabled` (par défaut : `false`, désactivé car FactionPlugin gère déjà ça correctement côté Paper quand on reste sur Factions).
- Format 100 % personnalisable avec `chat-faction-format` et placeholders dédiés (`%faction%`, `%faction_rank%`, `%faction_icon%`).
- Lecture des mêmes données que pour le tab (cache en mémoire rafraîchi par `FactionSync`) — pas de coût supplémentaire, même `refresh-interval-seconds`.
- Implémentation propre via `PlayerChatEvent.ChatResult.message(...)` — l'API officielle Velocity prévue pour ce cas d'usage, sans hook interne ni chat préempté.
- **Tag en texte brut uniquement** — `ChatManager.stripSpecialCharacters()` retire *tous* les `&x` et *tous* les `§` du texte injecté, ainsi que des noms de faction et icônes de rang. La réécriture envoie un message strictement plat au backend, qui le revalide comme s'il venait du client. Compatible avec **tous** les filtres anti-triche (pas de kick pour « caractères interdits dans le tchat »).
- **Ne s'applique que sur le serveur Factions** : sur les autres serveurs (lobby, minijeux…), le message passe tel quel, sans modification.

### 🧩 Autres commodités
- **Commande admin** : `/herotab reload` (alias `/htab`) pour recharger la configuration sans redémarrer le proxy. Annule et recrée proprement les tâches planifiées (update + intégrations MySQL), aucune fuite, aucune tâche orpheline.
- **Permission unique** : tout est sous `herotab.admin`.
- **`update-interval-ticks`** configurable, converti automatiquement côté Velocity en millisecondes (20 ticks = 1 s, minimum 200 ms).
- **Logs au démarrage** : nom du plugin, version, nombre de groupes de serveurs, état des intégrations MySQL activées.
- **Replay propre des animations** : l'index d'animation est dérivé de l'horloge système — pas de sauvegarde d'état nécessaire, redémarrer le proxy repart d'une animation cohérente.

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
git clone https://github.com/herocraftlol/HeroTab.git
cd HeroTab/herotab
mvn clean package
```

Le JAR shaded est disponible dans `herotab/target/herotab-2.0.0.jar`.

---

## 📂 Structure du projet

```
HeroTab/
├── LICENSE
├── README.md
└── herotab/
    ├── pom.xml
    └── src/main/
        ├── java/com/herocraft/herotab/
        │   ├── HeroTabPlugin.java          # Point d'entrée Velocity, scheduler, reload
        │   ├── TabListManager.java         # Cœur du plugin — header, footer, ordre, entrées
        │   ├── ChatManager.java            # Injection du tag de faction dans le chat
        │   ├── command/HeroTabCommand.java # /herotab reload
        │   ├── config/
        │   │   ├── ConfigManager.java      # Lecture/écriture config.yml
        │   │   └── HeroTabConfig.java      # Modèle + parsing YAML
        │   └── integration/
        │       ├── GradeSync.java          # Lecture MySQL GradePlugin
        │       ├── FactionSync.java        # Lecture MySQL FactionPlugin
        │       ├── GradeInfo.java / FactionInfo.java # Modèles records
        │       └── JdbcDriverLoader.java   # Workaround d'isolation classloader
        └── resources/config.yml            # Configuration par défaut, copie au premier lancement
```

---

## ⚙️ Configuration (extrait)

```yaml
update-interval-ticks: 20
animation-interval-ticks: 20
theme-primary: "&b"
theme-secondary: "&e"
network-address: "herocraft.servegame.com"
website-address: "herocraft.servegame.com"
factions-server-name: "factions"
sort-mode: "SERVER_SELF_FIRST"
reorder-mode: "safe"
group-spacer-enabled: true
group-spacer-text: "%secondary%&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
allow-minimessage: true

player-format: "&7[%server_color%%server%&7] %grade_prefix%&f%player%%faction_tag% &8•&7 %ping%ms"

header:
  - "%primary%&m                                        "
  - ""
  - "%primary%&lHeroCraft %secondary%&l• %primary%Le réseau"
  - "&7Réseau : %primary%%online%&7/%primary%%max% &8| &7Ici : %secondary%%server_online%"
  - ""
  - "%primary%&m                                        "

footer:
  - "%secondary%&m                                        "
  - ""
  - "&7Serveur : %primary%%network_address%"
  - "&7Site : %primary%%website_address%"
  - ""
  - "%secondary%&m                                        "

grades-mysql:
  enabled: false
  host: "127.0.0.1"
  port: 3306
  database: "grades_db"
  user: "grades_user"
  password: "mot-de-passe"
  refresh-interval-seconds: 15

factions-mysql:
  enabled: false
  host: "127.0.0.1"
  port: 3306
  database: "herocraft"
  user: "herocraft_user"
  password: "mot-de-passe"
  refresh-interval-seconds: 15

server-groups:
  bedwars1: "BedWars"
  bedwars2: "BedWars"
  bedwars3: "BedWars"
  hikabrain: "HikaBrain"
  hungergames: "HungerGames"
  factions: "Factions"

# Tag de faction dans le chat — texte brut, fonctionne sur tout le réseau
chat-faction-tag-enabled: false
chat-faction-format: "[%faction_icon%%faction%] "
```

📘 **Toutes les options sont documentées en commentaire dans le `config.yml` distribué avec le JAR** (`herotab/src/main/resources/config.yml`).

---

## 📚 Placeholders disponibles

| Catégorie | Placeholders |
|---|---|
| **Joueur** | `%player%` `%ping%` |
| **Serveur** | `%server%` `%server_color%` `%group%` `%server_online%` `%online%` `%max%` |
| **Réseau** | `%network_address%` `%website_address%` `%primary%` `%secondary%` |
| **Grade** | `%grade%` `%grade_prefix%` `%grade_suffix%` `%grade_color%` *(GradePlugin)* |
| **Faction (tab)** | `%faction%` `%faction_rank%` `%faction_tag%` *(FactionPlugin)* |
| **Chat faction** | `%faction%` `%faction_rank%` `%faction_icon%` *(sans couleur, texte brut)* |

> `%faction%`, `%faction_rank%` et `%faction_tag%` (tab) ne sont remplis **que si le viewer ET la cible sont actuellement sur le serveur `factions-server-name`** — ailleurs sur le réseau, ils restent vides (pas de spoil inter-serveurs). Même règle pour le tag de chat (`chat-faction-tag-enabled`).

---

## 🆕 Nouveautés de la v2.0.0

Cette version marque un **palier majeur** dans l'évolution d'HeroTab. Elle consolide toutes les fonctionnalités développées depuis la v1.0.0, durcit le comportement face aux skins et aux anti-triche, et prépare le plugin pour les réseaux HeroCraft de la saison prochaine.

### 🎯 Une base unifiée pour tout le réseau

Le tab est désormais **vraiment cohérent d'un sous-serveur à l'autre** : un joueur qui passe de Factions à BedWars en gardant son grade et sa faction n'a plus aucune rupture visuelle. Les grades, factions, et préfixes sont lus en arrière-plan, jamais en chemin critique.

- 🆙 **Version bumpée à 2.0.0** dans `pom.xml` et l'annotation `@Plugin` — toutes les surfaces de version (JAR, `velocity-plugin.json`, logs) restent synchronisées.
- 📝 **Description enrichie** dans `pom.xml` et l'annotation `@Plugin` : un seul coup d'œil dans le listing Velocity ou le panneau d'admin suffit désormais à comprendre ce que fait le plugin — tab unifié, animations, intégration MySQL, tag de chat anti-triche safe.
- 🏷️ **Descriptions alignées** entre `pom.xml`, `velocity-plugin.json`, `config.yml` et ce README — fini les incohérences d'un fichier à l'autre.
- 📦 **JAR shaded** : `mysql-connector-j` et `snakeyaml` restent embarqués, signature et métadonnées Maven à jour.

### 💬 Tag de faction dans le chat, durci et sécurisé

Le tag de faction a été retravaillé pour être **vraiment utilisable en production** sur tous les setups anti-triche. C'est la fonctionnalité phare de cette release.

- **Tag en texte brut uniquement** — `ChatManager.stripSpecialCharacters()` retire *tous* les `&x` et *tous* les `§` du texte injecté, ainsi que des noms de faction et icônes de rang si jamais ils en contenaient. Le tag est désormais strictement du texte plat.
- **Format par défaut minimaliste** : `chat-faction-format: "[%faction_icon%%faction%] "` — toujours lisible (`[★ MaFaction] Salut tout le monde !`), sans la moindre séquence `§` dedans.
- **Placeholders `chat-faction-format` réduits** à ceux qui ne produiraient que du texte plat : `%faction%`, `%faction_rank%`, `%faction_icon%`. Tout autre code (`&x`, `%primary%`, …) serait silencieusement filtré, donc autant ne pas les proposer.
- **Implémentation propre** : tout passe par `PlayerChatEvent.ChatResult.message(...)` (l'API officielle Velocity prévue pour ce cas d'usage). Aucun hook interne, aucun chat préempté, aucune bidouille de packets.
- **Refresh MySQL à la connexion** : `ServerConnectedEvent` force une relecture immédiate des caches Grade/Faction au lieu d'attendre le prochain cycle (jusqu'à 15 s). Les grades et factions apparaissent instantanément.

### 🛡️ Préservation des skins, encore plus robuste

- **`reorder-mode: safe`** (défaut) — on ne touche **jamais** à l'identité d'une entrée déjà présente. Seuls le texte et le ping sont rafraîchis → skins SkinRestorer / Bedrock / premium garantis intacts.
- **`reorder-mode: experimental`** — le vrai tri visuel est activé, mais le plugin ne retire/ré-ajoute les entrées **que quand l'ordre a réellement changé** (arrivée, départ, changement de serveur), pas à chaque cycle. Sur une version précédente, le retrait/ré-ajout à chaque tick empêchait le client de finir de charger les textures externes.
- **`lastOrderPerViewer`** : on garde en mémoire l'ordre appliqué pour chaque viewer, on ne recalcule les positions que si nécessaire. Sur les clients avec SkinRestorer, on stocke aussi le `GameProfile` complet en secours pour pouvoir recréer une entrée sans perdre le skin.

### 🔌 Intégrations MySQL durcies

- **Driver MySQL chargé explicitement** via `JdbcDriverLoader` (workaround du `No suitable driver found` récurrent sur les tâches schedulées Velocity).
- **`ServicesResourceTransformer`** activé dans le shade plugin pour préserver `META-INF/services/java.sql.Driver` après l'ombre.
- **SnakeYAML relogé** sous `com.herocraft.herotab.libs.snakeyaml` pour éviter tout conflit de classloader avec d'autres plugins Velocity qui embarqueraient leur propre version.

### 🧩 Administration et DX

- **Commande `/herotab reload`** (alias `/htab`) — recharge la configuration, recrée proprement les tâches planifiées, reconnecte les intégrations MySQL avec d'éventuels nouveaux identifiants, sans redémarrer le proxy.
- **Configuration YAML plate et lisible** : toutes les valeurs sont au premier niveau ou dans des listes/maps simples, pas d'objets imbriqués complexes. Format pensé pour un admin non-dev.
- **Validation au chargement** : valeurs absentes ou mal typées retombent silencieusement sur les défauts, le plugin ne refuse jamais de démarrer à cause d'un champ manquant dans la config.

---

## 🆕 Rappel des versions précédentes

### v1.4.0 — Chat faction fix (anti-cheat safe) 🐛
Correctif ciblé du tag de faction dans le chat introduit en v1.3.0. Le tag fonctionne désormais sur tout le réseau **sans déclencher les filtres anti-triche** des serveurs backend.

### v1.3.0 — Tag de faction dans le chat 💬
Introduction du `ChatManager` qui préfixe automatiquement chaque message des joueurs membres d'une faction. L'implémentation initiale envoyait encore des codes couleur `§` au backend et faisait kicker les joueurs sur les anti-triche standard — corrigé en v1.4.0.

### v1.2.0 — Consolidation & polish 🎯
Version synchronisée entre `pom.xml` et `@Plugin`, descriptions enrichies, README repensé pour la première fois avec sections « Pourquoi HeroTab ? » et tableau des placeholders.

### v1.1.0 — Commande `/herotab status` & polish 🩺
Constante `PLUGIN_VERSION` exposée publiquement, getters publics pour `GradeSync` et `FactionSync`, méthodes `cacheSize()` pour le diagnostic.

### v1.0.0 — Première publication officielle 🚀
Tab proxy-wide, mode `safe` pour préserver les skins, GradePlugin + FactionPlugin via MySQL, reload à chaud, fix du `No suitable driver found` dans les tâches schedulées.

---

## 🤝 Contribution

Les PR sont les bienvenues ! Pour proposer un changement :

1. Forkez le repo et créez une branche (`feature/ma-fonctionnalite`).
2. Committez vos changements.
3. Poussez et ouvrez une Pull Request en décrivant clairement la motivation et l'impact.

---

## 📜 Licence

Distribué sous licence **MIT**. Voir le fichier [LICENSE](LICENSE) pour le texte complet.

© 2026 HeroCraft — Tous droits réservés sur la marque et le nom du réseau.
