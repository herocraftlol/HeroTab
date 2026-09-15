<div align="center">

# 🟦 HeroTab

**Le tab list unifié, fluide et 100 % personnalisable du réseau HeroCraft.**

[![Version](https://img.shields.io/badge/version-3.0.0-00b4ff?style=flat-square)](#-nouveautés-de-la-v300)
[![Velocity](https://img.shields.io/badge/Velocity-3.3.0%2B-blueviolet?style=flat-square)](#-compatibilité)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)](#-compatibilité)
[![License](https://img.shields.io/badge/license-MIT-success?style=flat-square)](LICENSE)
[![Network](https://img.shields.io/badge/HeroCraft-Officiel-ff69b4?style=flat-square)](https://github.com/herocraftlol)

Plugin **Velocity** qui remplace la liste vanilla des joueurs par une expérience cohérente sur **tout le réseau HeroCraft** : un seul onglet « joueurs » pour tous les sous-serveurs Paper, avec header/footer animés, format de ligne et tri configurables, lecture directe MySQL pour GradePlugin et FactionPlugin, tag de faction dans le tab et le chat (limité au serveur Factions), et un mode `safe` qui préserve les skins (SkinRestorer, Bedrock, comptes premium) sans concession sur le rendu.

</div>

---

## 🤔 Pourquoi HeroTab ?

Par défaut, l'onglet « joueurs » de Minecraft n'est pas pensé pour les réseaux multi-serveurs : chaque backend Paper affiche son propre sous-ensemble de joueurs, sans les grades, sans la faction, sans aucune animation. Pour un réseau comme **HeroCraft**, qui regroupe Factions, BedWars, HikaBrain, HungerGames et d'autres sous-serveurs derrière un proxy Velocity, ça donne à l'arrivée :

- 😕 Un joueur Factions qui rejoint HikaBrain ne voit plus son tag de faction.
- 😕 Les joueurs de BedWars et HungerGames se « mélangent » en vrac, sans distinction visuelle du serveur.
- 😕 Les grades achetés sur GradePlugin (VIP, Premium…) n'apparaissent jamais dans le tab des autres serveurs.
- 😕 Sur un chat relayé entre sous-serveurs, le tag de faction du locuteur saute : FactionPlugin ne formate que les messages émis depuis le serveur Factions.
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
- **`reorder-mode: experimental`** — active le vrai tri visuel (sort-mode, group-spacer) pour les setups où les skins ne posent pas problème. Le plugin ne retire/ré-ajoute les entrées **que quand l'ordre a réellement changé** (arrivée, départ, changement de serveur), pas à chaque cycle, pour laisser le temps aux textures externes de se stabiliser. Le profil (skin) du joueur est lu directement via `target.getGameProfile()`, déjà résolu par Velocity / SkinRestorer dès la connexion : c'est fiable à 100 % pour chaque entrée, sans cache intermédiaire ni condition de course entre viewers.
- **Séparateur de groupe configurable** : `group-spacer-text` permet de définir le motif décoratif inséré entre chaque bloc de serveur en `SERVER` / `SERVER_SELF_FIRST`.

### 🔗 Intégrations MySQL natives (lecture seule)
- **GradePlugin** : lecture directe de `grades_db` (tables `player_grades` + `grades`), rafraîchissement périodique configurable. Placeholders `%grade%`, `%grade_prefix%`, `%grade_color%`, `%grade_suffix%` automatiquement remplis, avec `ORDER BY priority DESC` pour prendre le grade le plus prioritaire.
- **FactionPlugin** : lecture de la table `faction_tab_sync`. Le tag de faction (`%faction_tag%`) **n'est visible que si le viewer ET la cible sont sur le serveur Factions** — invisible partout ailleurs sur le réseau (pas de spoil inter-serveurs).
- **Driver MySQL embarqué** : `mysql-connector-j` est *shadé* dans le JAR final. Un chargeur dédié (`JdbcDriverLoader`) contourne le problème d'isolation de classloaders de Velocity pour que les connexions fonctionnent depuis une tâche planifiée (le `ServiceLoader` standard renvoie sinon `No suitable driver found`).
- **Refresh immédiat à la connexion** : `ServerConnectedEvent` force une relecture MySQL + un `updateAll()` du tab — fini les 15 s d'attente après un `/f join` ou un changement de grade.

### 💬 Tag de faction dans le chat (optionnel)
- **Activation** : `chat-faction-tag-enabled: true` dans `config.yml` (désactivé par défaut).
- **Comportement** : réécrit le message brut envoyé au backend via `PlayerChatEvent.ChatResult.message(...)` pour préfixer les messages des joueurs qui ont une faction. Utile quand tu relies plusieurs sous-serveurs et que le format local de FactionPlugin ne traverse plus (chat global, plugin de chat tiers, etc.).
- **Limité au serveur Factions** : le tag n'est ajouté QUE si le joueur parle depuis le sous-serveur dont l'id backend correspond à `factions-server-name`. Ailleurs (lobby, minijeux…) aucun préfixe n'est collé.
- **Safe vis-à-vis des anti-triche** : tout code couleur (`&x`) ou caractère `§` éventuellement présent dans la donnée source (nom de faction, icône de rang) est retiré automatiquement — les anti-triche backend considèrent en effet `§` comme un « caractère interdit » dans un message brut de chat. Le tag est donc en texte simple, sans couleur, garanti jamais à l'origine d'un kick « Caractères interdits ».
- **Placeholders** : `%faction%`, `%faction_rank%`, `%faction_icon%`. Le message original du joueur est ajouté tel quel juste après le préfixe.
- **Note technique** : la réécriture d'un message change sa signature cryptographique. Sur un setup Velocity + Paper moderne, Velocity bascule proprement vers un message non signé (cosmétique : une icône « non sécurisé » peut s'afficher sur certains clients si `enforce-secure-profile` est strict côté backend, sans blocage fonctionnel).

### 🧩 Autres commodités
- **Commande admin** : `/herotab reload` (alias `/htab`) pour recharger la configuration sans redémarrer le proxy. Annule et recrée proprement les tâches planifiées (update + intégrations MySQL), aucune fuite, aucune tâche orpheline.
- **Permission unique** : tout est sous `herotab.admin`.
- **`update-interval-ticks`** configurable, converti automatiquement côté Velocity en millisecondes (20 ticks = 1 s, minimum 200 ms).
- **Logs au démarrage** : nom du plugin, version, nombre de groupes de serveurs, état des intégrations MySQL activées.
- **Replay propre des animations** : l'index d'animation est dérivé de l'horloge système — pas de sauvegarde d'état nécessaire, redémarrer le proxy repart d'une animation cohérente.

---

## 🆕 Nouveautés de la v3.0.0

Cette version fait passer HeroTab en **v3 « skin-friendly »** : un nouveau palier qui consolide la gestion des skins dans le tab (déjà robuste depuis la v2.0.2) ET réintroduit le tag de faction dans le chat, en option, anti-triche-safe. C'est une mise à jour **« drop-in »** : remplacez le JAR et `/herotab reload`, aucune migration de config n'est nécessaire — les nouvelles options du `config.yml` apparaissent commentées et désactivées par défaut.

- 🛡️ **Skin handling consolidé** (la pièce maîtresse de cette v3) : le mode `experimental` du tab ne retire et recrée désormais **une entrée que lorsque l'ordre a réellement changé** (arrivée, départ, changement de serveur). À chaque cycle intermédiaire, on se contente de patcher le texte et le ping sur les entrées déjà en place — fini la destruction / recréation en boucle qui empêchait les skins externes (SkinRestorer, Bedrock, voire des comptes premium classiques) de finir de charger côté client. Le profil est lu directement via `target.getGameProfile()`, déjà résolu par Velocity / SkinRestorer à la connexion : fiable à 100 %, sans cache intermédiaire ni condition de course entre viewers.
- 💬 **Tag de faction dans le chat (réintroduit, opt-in)** : nouvelle classe `ChatManager` qui écoute `PlayerChatEvent` (en `PostOrder.LATE`) et réécrit le message via `ChatResult.message(prefix + original)`. Activable avec `chat-faction-tag-enabled: true`. Anti-triche-safe : tout code couleur / `§` est retiré automatiquement du préfixe pour ne jamais déclencher un kick « Caractères interdits dans le tchat ». Limité au serveur Factions (vérification `factions-server-name`) — partout ailleurs, le message passe tel quel. Désactivé par défaut : sur un setup HeroCraft classique, FactionPlugin gère déjà ça correctement avec couleur, donc HeroTab n'intervient que si tu relies plusieurs sous-serveurs et perds le format local.
- 🆙 **Version bumpée à 3.0.0** dans `pom.xml`, l'annotation `@Plugin` et le `velocity-plugin.json` généré — toutes les surfaces de version (JAR, panneau d'admin Velocity, logs) restent synchronisées.
- 📝 **Description enrichie et synchronisée** : la description détaillée mentionne maintenant explicitement le tag de faction dans **le tab et le chat**, limité au serveur Factions. Cohérence entre `pom.xml`, `@Plugin`, `velocity-plugin.json` et ce README.
- 📦 **JAR shaded reconstruit** : `mysql-connector-j` et `snakeyaml` (relocalisé sous `com.herocraft.herotab.libs.snakeyaml`) toujours embarqués. `META-INF/services/java.sql.Driver` correctement fusionné via `ServicesResourceTransformer` — le driver MySQL reste détectable depuis une tâche planifiée du proxy.
- 🔁 **100 % rétrocompatible** avec la v2.0.2 et toutes les versions précédentes : structure du `config.yml` inchangée, mêmes placeholders, même format de ligne, même mécanisme de tri / mode safe. Les deux nouvelles clés (`chat-faction-tag-enabled`, `chat-faction-format`) sont purement additives — aucune migration n'est nécessaire.

### Historique récent
- **v2.0.2** — descriptions alignées et gestion des skins simplifiée.
- **v2.0.1** — nettoyage repo (suppression de la classe `ChatManager` introduite en v2.0.0 mais hors périmètre de cette publication).
- **v2.0.0** — consolidation majeure, README repensé.
- **v1.4.0** — chat faction fix (anti-cheat safe).
- **v1.3.0** — tag de faction dans le chat.
- **v1.2.0** — consolidation & polish.
- **v1.1.0** — commande `/herotab status` & polish.
- **v1.0.0** — première publication officielle.

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
unzip HeroTab-3.0.0-source.zip
cd HeroTab/herotab
mvn clean package
```

Le JAR final sera dans `herotab/target/herotab-3.0.0.jar`.

---

## 📜 Licence

Distribué sous licence MIT — voir le fichier [LICENSE](LICENSE).

_Ce message a été préparé par un agent OpenHands au nom de l'équipe HeroCraft._
