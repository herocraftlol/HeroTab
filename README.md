# HeroTab

> Un **tab list unifié, fluide et entièrement personnalisable** pour tout le réseau HeroCraft, exécuté côté proxy Velocity.

HeroTab remplace la liste des joueurs vanilla par une expérience cohérente quel que soit le sous-serveur Paper sur lequel chaque joueur est connecté. Il enrichit l'affichage avec les grades (GradePlugin) et les factions (FactionPlugin) lus directement depuis MySQL, et permet à l'équipe admin de tout configurer dans un unique `config.yml` sans toucher au code.

---

## ✨ Fonctionnalités

- **Header & footer animés** — texte multi-lignes avec substitution de placeholders (`%online%`, `%server%`, `%network_address%`, etc.) et frames animées via la syntaxe `frame1||frame2||frame3`.
- **Format joueur configurable** — chaque ligne du tab peut afficher le serveur, le grade, le préfixe, la faction, le ping et tout autre placeholder de votre choix.
- **Ordre des joueurs** — modes `ALPHABETICAL`, `PING`, `SERVER`, `SERVER_SELF_FIRST` (ton serveur d'abord, séparateur décoratif entre les groupes) ou `NONE`.
- **Mode `safe` par défaut** — ne touche jamais aux skins (SkinRestorer, Bedrock, comptes premium), seul le texte et le ping sont rafraîchis. Le mode `experimental` active le vrai tri visuel pour les setups où les skins ne posent pas problème.
- **Intégration GradePlugin** — lit `grades_db` en lecture seule, affiche automatiquement `%grade%`, `%grade_prefix%`, `%grade_color%`, etc. Rafraîchissement périodique configurable.
- **Intégration FactionPlugin** — lit la table `faction_tab_sync`, n'affiche le tag de faction qu'aux joueurs présents sur le serveur Factions (les autres serveurs du réseau ne le voient pas).
- **Regroupement de sous-serveurs** — `bedwars1/2/3` peuvent tous apparaître sous le label « BedWars » via `server-groups`.
- **Thème centralisé** — change `theme-primary` et `theme-secondary` pour repeindre tout le tab d'un coup.
- **MiniMessage supporté** — `<gradient:#00c6ff:#0072ff>HeroCraft</gradient>` fonctionne en parallèle des codes `&` classiques (hex `&#RRGGBB` inclus).
- **Commande admin** — `/herotab reload` (alias `/htab`) pour recharger la configuration sans redémarrer le proxy.
- **Driver MySQL embarqué** — `mysql-connector-j` est *shadé* dans le JAR final, et un chargeur dédié contourne le problème d'isolation de classloaders de Velocity pour que les connexions MySQL fonctionnent depuis une tâche planifiée.

---

## 📦 Installation

1. Téléchargez la dernière version `herotab-X.Y.Z.jar` depuis la page **Releases**.
2. Déposez le JAR dans le dossier `plugins/` de votre proxy **Velocity**.
3. (Re)démarrez le proxy — un `plugins/herotab/config.yml` sera créé automatiquement.
4. Éditez la configuration, puis faites `/herotab reload` (permission `herotab.admin`).

> Compatibilité : Velocity 3.3.0+ · Java 17+ · Paper (côté sous-serveurs) avec GradePlugin et/ou FactionPlugin selon ce que vous voulez afficher.

---

## 🛠️ Compilation depuis les sources

```bash
git clone https://github.com/herocraftlol/HeroTab.git
cd HeroTab/herotab
mvn clean package
```

Le JAR shaded sera disponible dans `herotab/target/herotab-1.0.0.jar`.

---

## 📂 Structure du projet

```
herotab/
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
    └── resources/config.yml            # Configuration par défaut
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
  factions: "Factions"
```

Toutes les options sont documentées en commentaire dans le `config.yml` distribué avec le JAR.

---

## 🆕 Nouveautés de la v1.0.0

Première publication officielle. Cette version consolide toutes les briques pensées pour le réseau HeroCraft :

- 🚀 **Tab proxy-wide** : header, footer et ordre calculés une seule fois puis poussés à chaque joueur connecté, sans pic CPU quand le réseau grossit.
- 🛡️ **Mode `safe`** activé par défaut pour préserver les skins (SkinRestorer, Bedrock, comptes classiques).
- 🎨 **Thème & MiniMessage** : un seul couple `theme-primary` / `theme-secondary` repeint toute la déco ; balises `<gradient>...</gradient>` acceptées.
- 🔗 **GradePlugin + FactionPlugin** : lecture directe MySQL, sans dépendance plugin-side ni hook.
- 🧩 **Regroupement de sous-serveurs** et **séparateur de groupe** en mode `SERVER_SELF_FIRST`.
- ⚙️ **Reload à chaud** via `/herotab reload` — toutes les tâches planifiées sont recréées proprement.
- 🐛 **Fix MySQL en classloader isolé** : `JdbcDriverLoader` charge explicitement `com.mysql.cj.jdbc.Driver` pour contourner le `No suitable driver found` des tâches schedulées sous Velocity.

---

## 📜 Licence

Distribué sous licence propriétaire © HeroCraft. Voir le fichier `LICENSE` si présent, ou contactez l'équipe HeroCraft pour les conditions d'utilisation.