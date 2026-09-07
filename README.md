<div align="center">

# 🟦 HeroTab

**Le tab list unifié, fluide et 100 % personnalisable du réseau HeroCraft.**

[![Version](https://img.shields.io/badge/version-1.2.0-00b4ff?style=flat-square)](#-nouveautés-de-la-v120)
[![Velocity](https://img.shields.io/badge/Velocity-3.3.0%2B-blueviolet?style=flat-square)](#-compatibilité)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)](#-compatibilité)
[![License](https://img.shields.io/badge/license-MIT-success?style=flat-square)](LICENSE)
[![Network](https://img.shields.io/badge/HeroCraft-Officiel-ff69b4?style=flat-square)](https://github.com/herocraftlol)

Plugin **Velocity** qui remplace la liste vanilla des joueurs par une expérience cohérente sur **tout le réseau HeroCraft** — header/footer animés, format de ligne configurable, intégration directe MySQL pour GradePlugin et FactionPlugin, et un mode `safe` qui préserve les skins (SkinRestorer, Bedrock, comptes premium) sans concession sur le rendu.

</div>

---

## 🤔 Pourquoi HeroTab ?

Par défaut, l'onglet « joueurs » de Minecraft n'est pas pensé pour les réseaux multi-serveurs : chaque backend Paper affiche son propre sous-ensemble de joueurs, sans les grades, sans la faction, sans aucune animation. Pour un réseau comme **HeroCraft**, qui regroupe Factions, BedWars, HikaBrain, HungerGames et d'autres sous-serveurs derrière un proxy Velocity, ça donne à l'arrivée :

- 😕 Un joueur Factions qui rejoint HikaBrain ne voit plus son tag de faction.
- 😕 Les joueurs de BedWars et HungerGames se « mélangent » en vrac, sans distinction visuelle du serveur.
- 😕 Les grades achetés sur GradePlugin (VIP, Premium…) n'apparaissent jamais dans le tab des autres serveurs.
- 😕 Aucun header/footer animé, aucune info réseau, aucun branding central.

**HeroTab corrige tout ça côté proxy** : un seul onglet « joueurs » cohérent pour tout le monde, configurable depuis un simple `config.yml`, sans rien installer côté Paper.

---

## ✨ Fonctionnalités

### 🎨 Tab list personnalisable
- **Header & footer multi-lignes** avec substitution complète de placeholders (`%online%`, `%server%`, `%network_address%`, `%player%`…).
- **Frames animées** : la syntaxe `frame1||frame2||frame3` fait défiler automatiquement le texte.
- **Format joueur** : un patron unique (`player-format`) appliqué à toutes les entrées du tab — serveur, ping, grade, faction, préfixe…
- **Thème centralisé** : changer `theme-primary` / `theme-secondary` repeint toute la décoration d'un coup.
- **Codes `&` classiques** (`&b`, `&l`, `&7`…) **+ MiniMessage** (`<gradient:#00c6ff:#0072ff>…</gradient>`, hex `&#RRGGBB`) — ne mélangez pas les deux sur la même ligne.

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
- **`reorder-mode: experimental`** — active le vrai tri visuel (sort-mode, group-spacer) pour les setups où les skins ne posent pas problème.

### 🔗 Intégrations MySQL natives
- **GradePlugin** : lecture directe de `grades_db` (tables `player_grades` + `grades`), rafraîchissement périodique configurable. Placeholders `%grade%`, `%grade_prefix%`, `%grade_color%`, `%grade_suffix%` automatiquement remplis.
- **FactionPlugin** : lecture de la table `faction_tab_sync`. Le tag de faction (`%faction_tag%`) **n'est visible que sur le serveur Factions** — invisible partout ailleurs sur le réseau (pas de spoil inter-serveurs).
- **Driver MySQL embarqué** : `mysql-connector-j` est *shadé* dans le JAR final. Un chargeur dédié (`JdbcDriverLoader`) contourne le problème d'isolation de classloaders de Velocity pour que les connexions fonctionnent depuis une tâche planifiée.

### 🧩 Autres commodités
- **Regroupement de sous-serveurs** : `bedwars1`, `bedwars2`, `bedwars3` peuvent tous apparaître sous le label « BedWars » via `server-groups`.
- **Séparateur de groupe** : une ligne décorative configurable (`group-spacer-text`) insérée entre ton serveur et les autres en mode `SERVER_SELF_FIRST`.
- **Commandes admin** : `/herotab reload` (alias `/htab`) pour recharger la configuration sans redémarrer le proxy.
- **Recharge à chaud propre** : `/herotab reload` annule et recrée proprement les tâches planifiées (update + intégrations MySQL), aucune fuite, aucune tâche orpheline.
- **Permission unique** : tout est sous `herotab.admin`.

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

Le JAR shaded est disponible dans `herotab/target/herotab-1.2.0.jar`.

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

player-format: "&7[%primary%%server%&7] %grade_prefix%&f%player%%faction_tag% &8•&7 %ping%ms"

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
```

📘 **Toutes les options sont documentées en commentaire dans le `config.yml` distribué avec le JAR** (`herotab/src/main/resources/config.yml`).

---

## 📚 Placeholders disponibles

| Catégorie | Placeholders |
|---|---|
| **Joueur** | `%player%` `%ping%` |
| **Serveur** | `%server%` `%group%` `%server_online%` `%online%` `%max%` |
| **Réseau** | `%network_address%` `%website_address%` `%primary%` `%secondary%` |
| **Grade** | `%grade%` `%grade_prefix%` `%grade_suffix%` `%grade_color%` *(GradePlugin)* |
| **Faction** | `%faction%` `%faction_rank%` `%faction_tag%` *(FactionPlugin)* |

> `%faction%`, `%faction_rank%` et `%faction_tag%` ne sont remplis **que pour les joueurs actuellement sur le serveur `factions-server-name`** — ailleurs sur le réseau, ils restent vides (pas de spoil inter-serveurs).

---

## 🆕 Nouveautés de la v1.2.0

Cette version consolide l'expérience HeroTab en une ligne de release claire et facilite le déploiement sur le réseau :

- 🆙 **Version bumpée à 1.2.0** dans `pom.xml` et l'annotation `@Plugin` — toutes les surfaces de version (JAR, `@Plugin`, logs) restent synchronisées.
- 📝 **Description enrichie** dans `pom.xml` et l'annotation `@Plugin` : un seul coup d'œil dans le listing Velocity ou le panneau d'admin suffit désormais à comprendre ce que fait le plugin (header/footer animés, GradePlugin, FactionPlugin, mode safe).
- 📦 **JAR shaded republié** avec `mysql-connector-j` et `snakeyaml` embarqués, signature et métadonnées Maven à jour.
- 📚 **README repensé et clarifié** : sections « Pourquoi HeroTab ? », tableau complet des placeholders, encart explicatif sur le mode `safe`, instructions d'installation et de compilation détaillées.
- 🔁 **Aucune modification de format `config.yml`** — la v1.2.0 reste **100 % rétrocompatible** avec les v1.0.0 et v1.1.0. Une mise à jour ne demande qu'à remplacer le JAR.

### Nouveautés de la v1.1.0 (rappel)

- 🩺 Commande `/herotab status` pour diagnostiquer l'état du plugin.
- 📌 Constante `PLUGIN_VERSION` exposée publiquement (`getPluginVersion()`).
- 📝 Le log de démarrage mentionne désormais la version.
- 🔌 Getters publics pour `GradeSync` et `FactionSync`.
- 🧹 Méthodes `cacheSize()` sur les deux syncers.

### Nouveautés de la v1.0.0 (publication initiale)

- 🚀 **Tab proxy-wide** : header, footer et ordre calculés une seule fois par cycle puis poussés à chaque joueur connecté.
- 🛡️ **Mode `safe`** activé par défaut pour préserver les skins (SkinRestorer, Bedrock, comptes classiques).
- 🎨 **Thème & MiniMessage** : un seul couple `theme-primary` / `theme-secondary` repeint toute la déco ; balises `<gradient>…</gradient>` acceptées.
- 🔗 **GradePlugin + FactionPlugin** : lecture directe MySQL, sans dépendance plugin-side ni hook.
- 🧩 **Regroupement de sous-serveurs** et **séparateur de groupe** en mode `SERVER_SELF_FIRST`.
- ⚙️ **Reload à chaud** via `/herotab reload` — toutes les tâches planifiées sont recréées proprement.
- 🐛 **Fix MySQL en classloader isolé** : `JdbcDriverLoader` charge explicitement `com.mysql.cj.jdbc.Driver` pour contourner le `No suitable driver found` des tâches schedulées sous Velocity.

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
