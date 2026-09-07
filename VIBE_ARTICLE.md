# Matériel d'article — développer une implémentation JPA avec un agent de code

> **Statut : brouillon de travail, pas un document du dépôt.** Rédigé en
> français par exception à la règle « tout `.md` en anglais » — c'est de la
> matière première pour ton article, pas de la documentation. À sortir du
> dépôt ou à ne pas commiter tel quel.
>
> **Chaque chiffre porte sa provenance.** `[mesuré]` = re-dérivé des journaux
> pendant la préparation de ce document, commande de vérification donnée.
> `[relevé]` = vient de ton propre relevé, non re-dérivé ici. `[doc]` = extrait
> de `HOW_TO_DEV_WITH_MISTRAL.md`. Ne publie pas un `[relevé]` sans le
> revérifier : c'est précisément la thèse de l'article.

---

## 1. La thèse, en une phrase

Un agent de code ne coûte pas ce qu'on croit, ne fait pas ce qu'on croit, et
la seule façon de le savoir est de lire ses journaux — pas ses résumés.

Trois corollaires, chacun contre-intuitif, chacun mesuré :

1. **La facture, c'est le contexte réexpédié, pas la génération.** Ratio
   entrée/sortie **238:1**. Changer de modèle de raisonnement déplace ~1 % de
   la note.
2. **Un agent rapporte son propre travail, et se trompe.** 24 cartes marquées
   DONE pendant que le compteur de conformité ne bougeait pas. 262 builds
   enregistrés en succès dont 38 avaient échoué.
3. **Une règle dans un prompt n'est pas une règle.** Elle ne devient un fait
   qu'à la couche qui peut réellement l'exprimer — et certaines couches ne le
   peuvent structurellement pas.

---

## 2. Chronologie

| Date | Événement |
| --- | --- |
| 2026-05-04 | Départ des deux branches JPA depuis `main` `[mesuré]` |
| → 2026-09-03 | `ybl/jpa-opencode` — modèles locaux (Qwen3.6-35B-A3B sur oMLX, pilotés par OpenCode). 283 commits. Abandonnée. `[mesuré]` |
| 2026-09-03 | `ybl/jpa-vibe` — passage à Mistral Vibe. Harness V2 : 8 agents, Mistral Medium 3.5 + Small 4 `[doc]` |
| 2026-09-04 | GLM 5.2 remplace Medium 3.5 pour le raisonnement `[doc]` |
| 2026-09-06 matin | Relevé de coût. Reconfiguration V3 : 5 agents, hook `pre_tool`, scripts de build `[mesuré]` |
| 2026-09-06 après-midi | Deux sessions sur le harness V3 `[mesuré]` |

Trois jours de travail effectif sur Vibe. 248 commits sur la branche.

```bash
git log --reverse --format='%ad' --date=short ybl/jpa-vibe | head -1
git rev-list --count ybl/jpa-vibe
```

**Le banc d'essai** : Jakarta Persistence 3.2 — JPA classique — dans
l'écosystème Vidocq. Contraintes dures : Java 25, modules Java stricts, zéro
dépendance runtime hors API Jakarta, aucune réflexion runtime sur les classes
utilisateur, aucune génération de bytecode à chaud, TDD. Et un TCK officiel
comme juge : **1 745 tests**, une métrique que l'agent ne peut pas négocier.

C'est ce dernier point qui rend l'histoire intéressante : il existe un oracle
externe. La plupart des retours d'expérience sur les agents de code n'en ont
pas, et se réduisent à « ça avait l'air de marcher ».

> ⚠️ **Deux compteurs TCK à ne pas confondre.** Le TCK Jakarta Persistence 3.2
> contient **~1 745 méthodes de test réparties sur 269 classes clientes**
> `[doc]`. La tentative locale, elle, rapportait `991 run, 989 errors` — c'est
> ce que *son* runner exécutait, pas la taille du TCK. Si tu cites un
> dénominateur dans l'article, dis lequel des deux.

---

## 3. Angle A — le coût réel

### Les chiffres

10 sessions, 3–6 septembre 2026 `[relevé]` :

| | |
| --- | --- |
| Coût | **43,98 $** |
| Cartes livrées | 24 sur 112 |
| Steps | 1 701 |
| Tokens entrée / sortie | 138 M / 581 k — **238:1** |
| Entrée servie par le cache | 90,6 % |
| Contexte moyen par step | **81 000** |
| Part de la facture en entrée | **93 %** |

Re-dérivé indépendamment `[mesuré]` : 11 sessions, contexte moyen de fin de
session **86 165**, 181,6 steps de moyenne. Cohérent avec ton relevé.

```bash
python3 -c "
import json,glob
r=[json.load(open(m)) for m in glob.glob('$HOME/.vibe/logs/session/*/meta.json')]
r=[d for d in r if d.get('git_branch')=='ybl/jpa-vibe']
print(len(r),'sessions',sum(d['stats']['steps'] for d in r),'steps')
print('ctx moyen',sum(d['stats']['context_tokens'] for d in r)//len(r))"
```

### L'ordre des priorités, et pourquoi il surprend

1. **Volume de contexte réexpédié** — −70 % de contexte ≈ −65 % de facture.
2. **Part des steps portée par le petit modèle** — 11× moins cher en *entrée*.
3. **Choix du modèle de raisonnement — environ 1 %.**

Le troisième point est celui qui fait un bon paragraphe. GLM 5.2 et Mistral
Medium 3.5 ont des prix d'**entrée** quasi identiques (1,54 $ contre 1,50 $ par
million). Comme 93 % de la facture est de l'entrée, arbitrer entre eux sur le
coût n'a aucun sens. Le passage à GLM était une décision de **qualité et de
fenêtre de contexte** (1 M contre 256 k) — jamais une économie, et il ne faut
pas le raconter comme telle.

> **Angle d'attaque** : tout le discours public sur le coût des agents porte
> sur le prix de sortie des modèles. C'est la colonne qui ne compte pas.

### Où passait réellement le contexte `[mesuré]`

39 transcrits, parents et sous-agents :

| Source, contexte primaire | Part |
| --- | --- |
| `read_file` | 42 % |
| `bash` — sorties de build, recherches | 28 % |
| `write_file` + `edit` | 21 % |
| `task` — délégation réelle | **4,6 %** |

**72 % du contexte primaire était du travail délégable que l'agent principal
faisait lui-même.** C'est le chiffre central de l'article.

L'asymétrie qui explique tout : **le contexte d'un sous-agent est jeté au
retour ; celui du primaire est réexpédié à chaque step jusqu'à la fin de la
session.** Un fichier lu par le primaire au step 3 est encore facturé au step
297.

### La session à 19,55 $

Une seule session a coûté **19,55 $ — 44 % du budget de trois jours** `[relevé]`.
757 steps sur un seul sujet, sans jamais repartir de zéro `[mesuré]`. C'est
l'argument pour « une session = une carte », et il se raconte tout seul.

---

## 4. Angle B — mesurer au lieu de croire

C'est le meilleur angle. Chaque élément est une petite histoire complète.

### 4.1 Les 262 builds « réussis »

262 invocations Maven, **toutes enregistrées `exit_code: 0`**. Dans leurs
sorties : 38 `BUILD FAILURE`, 3 erreurs de compilation, 17 échecs de tests
`[relevé]`.

La cause tient en une ligne. Le motif employé partout était :

```bash
mvn … 2>&1 | tail -50
```

Dans un pipeline, `$?` est le statut de **la dernière** commande — `tail`, qui
réussit toujours. Le verdict de Maven était détruit avant que quiconque puisse
le lire. Conséquence : l'agent devait **lire de la prose** pour savoir s'il
venait de casser le build, et la règle « on ne commite que si le build passe »
n'était pas mécaniquement vérifiable.

**La démonstration, reproductible en 30 secondes** — projet jetable avec une
erreur de compilation volontaire `[mesuré]` :

| | code retour |
| --- | --- |
| `mvn … 2>&1 \| tail -5` sur un build cassé | **0** ← le bug |
| `./scripts/build.sh` sur le même build cassé | **1**, `BUILD_RESULT=FAILURE` |
| `./scripts/build.sh` après correction | **0**, `BUILD_RESULT=SUCCESS` |

> **Scène à raconter** : pendant que je vérifiais ce correctif, j'ai lancé
> `./scripts/verify.sh … | tail -25` et le shell m'a répondu `EXIT=0` alors que
> le script sortait en 1. J'ai reproduit le bug que je venais de corriger, en
> le vérifiant. Ce qui a sauvé la lecture, c'est la ligne `BUILD=RED
> exit_code=1` que le script imprime : elle survit au pipe, contrairement au
> code retour. C'est exactement pour ça qu'elle existe.

### 4.2 La même commande, 75 fois

218 appels à `find`, 124 distincts. La commande
`find … -name "EntityModel.java"` a été lancée **75 fois** dans les sessions,
répartie sur **trois orthographes** ne différant que par leur redirection
d'erreur : `2>/dev/null`, `2>&1`, et rien `[mesuré]`.

Deux leçons pour l'article :

- Un agent qui cherche un fichier ne se souvient pas qu'il l'a déjà cherché.
- **Une déduplication par comparaison de chaînes exacte n'en aurait attrapé
  aucune.** Il faut normaliser : retirer le `cd X &&` de tête, le proxy `rtk`,
  les redirections de queue. 730 des 1 312 appels bash passaient par un proxy.

Le correctif n'est pas « dire à l'agent de ne pas chercher ». C'est que le
parent lui passe les **chemins absolus** dans la consigne de délégation.

### 4.3 40 % des lectures étaient des relectures

476 appels à `read_file`, dont **190 (40 %) relisaient un fichier déjà lu dans
la même session, inchangé** `[mesuré]`. Records : `MansartPersistenceProcessor.java`
24 fois, `pom.xml` 23 fois, `PLAN.md` 22 fois — dans une seule session.

Comme `read_file` pesait 42 % du volume, c'est **le plus gros levier isolé**,
et il ne figurait dans aucune de mes hypothèses de départ. Trouvé en écrivant
le script d'analyse, pas en réfléchissant.

La règle qui en découle est falsifiable : refuser une relecture seulement si
`(chemin, mtime, taille, fenêtre offset/limit)` est identique — donc si le
résultat serait **octet pour octet** le même. Impossible de bloquer une
lecture légitime.

### 4.4 24 cartes DONE, un compteur figé

Sur trois jours, **24 cartes marquées DONE pendant que le compteur de
conformité restait à 2 passés / 0 échoués / 2 en erreur** `[relevé]`.

Et sur la tentative précédente, la version encore plus nette `[doc]` : la carte
`JP-20` porte, **dans la même entrée**, une ligne « preuve » affirmant
*« 257 methods PASS »* et une ligne « notes » disant *« TCK 991 run, 989
errors… same baseline »*. Une contradiction interne qu'une session fraîche
lisant seulement `STATUS.md` ne pouvait pas détecter.

> **La citation de l'article** : un agent qui rapporte son propre travail
> n'est pas une mesure, c'est une espérance.

D'où l'agent `verify` : il exécute le build et les suites, ne peut **rien**
écrire, et il est la seule source autorisée des chiffres de `STATUS.md`. La
mesure doit venir de quelque chose qui n'a aucun intérêt à ce qu'elle soit
bonne.

Détail technique qui compte : `verify.sh` compte les tests dans le **XML
surefire**, pas dans la sortie console — un build peut afficher `BUILD SUCCESS`
alors qu'une suite a été sautée. `tests=0` après un build vert est traité
comme un échec.

### 4.5 Mes propres erreurs, corrigées par la mesure

À mettre en avant : l'article est plus crédible si l'auteur de l'analyse s'y
inclut.

- **J'ai annoncé « −59 % de contexte au premier essai ».** Le chiffre venait
  d'une session **encore en cours** — lue au step 25 de ce qui est devenu 297.
  Le vrai résultat est **−40 %**. *Un chiffre pris sur une session vivante
  n'est pas une mesure* — la même erreur, en miniature, que celle que tout ce
  harness combat.
- **J'ai mis `bash` en `ask` pour garantir l'inviolabilité du TCK.** C'était
  vrai au moment où je l'ai écrit, et faux vingt minutes plus tard : j'avais
  ajouté au hook une règle « chemin TCK + écriture », et **les hooks
  s'exécutent avant l'invite de permission**. `ask` ne protégeait donc plus
  rien, et coûtait une invite par commande sortant du répertoire de travail.
  Leçon généralisable : *mettre la garantie dans la couche qui peut
  l'exprimer, puis arrêter de la payer deux fois.*
- **J'ai conclu trop vite qu'un correctif ne marchait pas.** Les invites
  continuaient. Vérification : la session Vibe avait démarré à 11:25:59, le
  correctif était daté 11:59:45. Elle tournait l'ancienne configuration. Rien
  n'était cassé, il fallait juste `/reload`.

---

## 5. Angle C — concevoir un harness, pas un prompt

### Les quatre couches, et ce que chacune peut réellement garantir

| Couche | Garantit | Ne peut pas garantir |
| --- | --- | --- |
| **Denylist de chemins** sur `write_file`/`edit` | Aucune écriture dans un `*-tck/`. Un match renvoie `NEVER`, final, bat tout le reste. | Ce qui passe par un shell — elle ne voit jamais une redirection. |
| **Hook `pre_tool`** | La chaîne de commande brute : build pipé, écriture TCK par le shell, dump d'archive, recherche répétée, relecture inchangée. | Ce qu'on n'a pas écrit. Échoue **ouvert** par conception. |
| **Permission d'outil** | `never` bloque, `ask` interrompt un humain. | Rien de sémantique. |
| **`allowed_models`** | `/model` et tout repli ne peuvent atteindre que les alias listés. | — |

### Le point technique le plus intéressant de toute l'histoire

**Certaines règles sont structurellement inexprimables dans le système de
permissions.**

`tools.bash.denylist` compare des **préfixes de commande** à des morceaux
qu'un parse tree-sitter a **déjà découpés sur les pipes**. `mvn … | tail`
devient deux parties indépendantes : `mvn …` et `tail`. Aucune règle de
permission ne peut donc exprimer « `mvn` ne doit pas être pipé » — **elle ne
voit jamais le pipe**.

C'est un excellent moment d'article : la fonctionnalité de sécurité est
correctement conçue (découper sur les pipes évite qu'une commande dangereuse
se cache derrière une commande anodine) et cette conception même rend la
règle qu'on veut écrire impossible. Il faut un hook `pre_tool`, qui reçoit
`tool_input.command` intact.

### Ce qui n'existe pas dans l'outil (vibe 2.24.5) `[mesuré]`

Vérifié dans le paquet installé, sans contournement inventé :

- Plafond de steps par agent ou sous-agent — **n'existe pas**. `max_turns`
  n'est ni une clé de config ni un override d'agent : uniquement le flag CLI
  `--max-turns`, en mode programmatique.
- Plafond de tokens ou de coût par session — CLI uniquement.
- Réglage de mise en cache de préfixe — n'existe pas.
- Budget de tokens de raisonnement — n'existe pas.
- Désactiver la compaction automatique — n'existe pas ; seul le seuil bouge.

> **Angle** : ce que ces absences disent du niveau de maturité des CLI
> agentiques en 2026. Le garde-fou le plus utile — un plafond d'itérations par
> sous-agent — est celui qu'aucun des outils que j'ai essayés ne propose.

### Le piège du raisonnement

Le raisonnement est une propriété **du modèle**, pas de l'agent. Pour obtenir
« désactivé par défaut, activable explicitement », il faut déclarer **deux
entrées de modèle sur le même modèle d'API**, sous deux alias.

Et une trouvaille en lisant le backend : les cinq niveaux de `thinking`
proposés se replient sur **deux** valeurs côté API. `medium`, `high` et `max`
donnent tous `high` ; `low` donne `none`. **`max` n'achète rien**, et `low`
est en réalité « désactivé », juste mal nommé.

### La délégation comme stratégie de cache

Il n'existe aucun réglage de cache. Le cache se déclenche sur un **préfixe de
requête stable**. Donc : garder `AGENTS.md` court et stable, ne pas remplacer
le prompt système du primaire, et pousser tout ce qui est volatil dans des
sous-agents. **La délégation *est* la stratégie de cache.** C'est une jolie
convergence : la même décision optimise le coût, la qualité et le cache.

---

## 6. Angle D — trois tentatives sur la même spec

### Tentative 1 — modèles locaux

`ybl/jpa-opencode` : Qwen3.6-35B-A3B sur oMLX, piloté par OpenCode. 283
commits, du 4 mai au 3 septembre `[mesuré]`.

Résultat `[doc]` : **79 cartes sur 95 planifiées (83 %)** sur M0–M4, 315 tests
unitaires qui passent dans `mansart-persistence-core` — et **le provider TCK
officiel n'a jamais été branché à une implémentation réelle**. Dernière mesure :
`991 run, 989 errors, 2 skipped`, **identique à la ligne de base M0**, tout
échouant dans `PMClientBase.setup()` parce que l'unité de persistance du TCK
pointait encore sur un stub.

**83 % d'avancement affiché, 0 % d'avancement réel sur la seule métrique qui
compte.** C'est le titre de section qui s'écrit tout seul.

Autre observation transférable `[doc]` : sur ce modèle local, la compaction
automatique dégradait mesurablement la session — **0 % de taux d'appel d'outil
après la quatrième compaction**, l'agent se mettant à *raconter* les appels
d'outils au lieu de les émettre. Symptôme à surveiller : les tournures « Let me
now… », « I'll load the skill and then: ».

### Tentative 2 — Vibe V2

8 agents, Medium 3.5 puis GLM 5.2, prompts soignés, contrat d'ingénierie
détaillé. C'est la tentative qui a produit les 43,98 $ et les 24 cartes. Elle
marchait — et c'est justement pour ça qu'elle est intéressante : **rien n'était
visiblement cassé**. Il a fallu lire les journaux pour voir les 75 `find`, les
40 % de relectures et les 262 builds mal évalués.

### Tentative 3 — Vibe V3

5 agents, raisonnement désactivé par défaut, hook `pre_tool`, scripts de build,
notes de spec condensées, une session = une carte.

**Résultats après deux sessions** `[mesuré]` :

| | V2 — 11 sessions | V3 — 2 sessions |
| --- | --- | --- |
| Contexte par session | 86 165 moyen | **52 087 moyen** |
| Sous-agents par session | 2,9 moyen | **41 et 4** |
| Refus du garde-fou | — | **50** |

**−40 % de contexte, délégation multipliée par ~14** sur la session longue.

Et le meilleur détail : **ce que le garde-fou a refusé** `[mesuré]` —

| Refusé | Nombre |
| --- | --- |
| Recherche déjà lancée dans la session | 20 |
| Build pipé, code retour détruit | 12 |
| Relecture d'un fichier inchangé | 10 |
| `mvn` appelé directement | 6 |
| `cat fichier \| tail` | 1 |
| Sortie de build écrite dans `/tmp` | 1 |

L'agent a continué à tenter **exactement** les comportements que la mesure
avait prédits. Ces règles ne décrivent pas un risque théorique : non
appliquées, c'est simplement ce que le modèle fait.

### Les pièges de nommage des modèles `[doc]`

Bon encadré, court et utile :

- **Devstral** — le nom « code » évident. **Retiré** de l'API hébergée : tous
  les SKU sont au-delà de leur date de retrait (Small 1.0 en 2025-11-30,
  Medium 1.0 et Small 1.1 en 2026-05-31, Small 2 en 2026-03-31, Devstral 2 en
  2026-07-31). Poids ouverts, donc toujours utilisable en local.
- **Mistral Large 3** — le nom « raisonnement » évident. Réel et courant, mais
  **moins cher** que Medium 3.5, livré **sans mode raisonnement**, et perdant
  de 21 à 29 points contre Medium sur les benchmarks agentiques les plus
  proches. Rejeté.
- **Mistral Small 4** — le nom « petites tâches ». Correspond exactement à ce
  qu'on attendait ; c'est la recommandation de Mistral pour ce rôle.
- **GLM 5.2** — 744 B de paramètres MoE, 40 B actifs, licence MIT, hébergé par
  Mistral depuis 2026-08-06. N'apparaît **pas** dans la liste statique de
  modèles de Vibe : il n'arrive que par une couche de routage dynamique.

> **Leçon** : la hiérarchie Small / Medium / Large **n'implique pas** une
> hiérarchie de capacité. Vérifier le cycle de vie, le prix et le
> positionnement avant d'attribuer un rôle. Deux des trois choix « évidents »
> étaient faux.

---

## 7. La scène finale — l'agent qui commite du code mort

À raconter en clôture, parce qu'elle résume tout.

Une fois le harness V3 en place, une session a implémenté la carte M4-JP-26 et
l'a commitée : `feat: implement M4-JP-26 persistence context state machine and
identity map`. Message propre, carte marquée faite.

Sauf que les fichiers ont été écrits dans **deux répertoires racine sans
`pom.xml`, absents des `<module>` du reactor** — le préfixe
`mansart-jakarta-persistence/` manquait. Résultat `[mesuré]` :

- `MansartPersistenceContext.java` existe en deux exemplaires **différents** :
  484 lignes commitées dans l'orphelin, 298 lignes non commitées au bon
  endroit ;
- le code commité **n'est jamais compilé** — il n'appartient à aucun module ;
- le build était **rouge** au bon endroit, sur un test qui ne compilait pas.

**Le build passait précisément parce que le code n'était compilé nulle part.**

Ce que ça dit : le harness a attrapé six catégories de dérive, et en a laissé
passer une septième à laquelle personne n'avait pensé — écrire hors du reactor.
La conclusion honnête n'est pas « j'ai résolu le problème », c'est *« chaque
garde-fou est la trace d'un échec déjà survenu, et la liste n'est jamais
close »*.

Vérification :

```bash
ls -d mansart-persistence-core mansart-persistence-tests   # sans pom.xml
grep -o '<module>[^<]*' pom.xml                            # ils n'y sont pas
```

---

## 8. Encadré — les chiffres, avec leur provenance

| Chiffre | Valeur | Source |
| --- | --- | --- |
| Coût 3 jours | 43,98 $ | `[relevé]` |
| Cartes livrées | 24 / 112 | `[relevé]` |
| Steps | 1 701 | `[mesuré]` |
| Ratio entrée:sortie | 238:1 | `[relevé]` |
| Part entrée dans la facture | 93 % | `[relevé]` |
| Contexte moyen par step | 81 000 / 86 165 | `[relevé]` / `[mesuré]` |
| Session la plus chère | 19,55 $ (44 %), 757 steps | `[relevé]` / `[mesuré]` |
| `read_file` dans le contexte primaire | 42 % | `[mesuré]` |
| Délégation réelle (`task`) | 4,6 % | `[mesuré]` |
| Travail délégable non délégué | 72 % | `[mesuré]` |
| `find` identiques | 75 (3 orthographes) | `[mesuré]` |
| Relectures inchangées | 190 / 476 = 40 % | `[mesuré]` |
| Builds mal évalués | 38 / 262 | `[relevé]` |
| Cartes DONE, compteur figé | 24 | `[relevé]` |
| Tentative locale | 83 % de cartes, TCK inchangé | `[doc]` |
| Gain V3 sur le contexte | −40 % | `[mesuré]` |
| Refus du garde-fou | 50 en 2 sessions | `[mesuré]` |

**Avant publication, re-dérive les `[relevé]`.** Notamment 43,98 $, 19,55 $,
38/262 et 93 % — je ne les ai pas recalculés moi-même, et publier un chiffre
non vérifié dans un article dont la thèse est « mesurez » serait fâcheux.

---

## 9. Ce qu'il ne faut PAS affirmer

- ❌ « Passer à GLM 5.2 a réduit les coûts. » **Faux** — prix d'entrée quasi
  identiques, et l'entrée est 93 % de la facture. C'était un choix de qualité
  et de fenêtre de contexte.
- ❌ « Le harness a résolu le problème. » Deux sessions, −40 %, cible à 30 000
  non atteinte. `read_file` pesait encore 51 % et 71 % du volume.
- ❌ « Les garde-fous font l'économie. » Rejeu contrefactuel sur les
  transcrits : **les garde-fous valent 12 %**, la délégation fait le reste, et
  ni l'un ni l'autre n'atteint la cible sans terminer la session après une
  carte. La configuration ne peut pas forcer cette dernière partie.
- ❌ « Les modèles locaux ne marchent pas. » La tentative locale a produit 315
  tests qui passent. Elle a échoué sur la **traçabilité de l'avancement**, pas
  sur la génération de code.
- ⚠️ Deux sessions ne sont pas une tendance. Le dire dans l'article.

---

## 10. Titres et accroches possibles

**Titres**
- « 43,98 $, 24 cartes, et 262 builds qui mentaient »
- « Ce que j'ai trouvé en lisant les journaux de mon agent de code »
- « 238:1 — pourquoi vous optimisez le mauvais côté de la facture »
- « Une règle dans un prompt n'est pas une règle »
- « 83 % d'avancement, 0 % de progrès »

**Accroches**
- « Mon agent a lancé la même commande `find` soixante-quinze fois. Je ne l'ai
  su qu'en écrivant un script pour lire ses propres journaux. »
- « Pendant trois jours, 262 builds ont été enregistrés en succès. Trente-huit
  avaient échoué. La cause tient dans un caractère : un pipe. »
- « Le meilleur garde-fou que j'aie écrit protège l'agent contre une chose
  qu'il refaisait vingt fois par session : chercher un fichier qu'il avait
  déjà trouvé. »

---

## 11. Plans possibles

**Plan A — enquête (recommandé)**
Ouvrir sur les 262 builds → la facture et le ratio 238:1 → où passe vraiment le
contexte → les quatre couches d'application → l'agent qui commite du code mort
→ conclusion : chaque garde-fou est la trace d'un échec.

**Plan B — chronologique**
Trois tentatives, ce qui a échoué à chaque fois, ce qui a été transféré.
Meilleur si tu veux parler de Vidocq et de JPA autant que des agents.

**Plan C — technique court**
Uniquement l'angle C : les couches d'application, l'inexprimabilité liée au
découpage sur les pipes, le hook `pre_tool`. Public plus étroit, plus dense.

---

## 12. Sources reproductibles

| Quoi | Où |
| --- | --- |
| Journaux de session | `~/.vibe/logs/session/*/meta.json`, `messages.jsonl`, `agents/**/messages.jsonl` |
| Champs utiles | `stats.steps`, `context_tokens`, `session_prompt_tokens`, `session_cached_tokens`, `tool_calls_hook_denied` |
| Harness documenté | `mansart/VIBE_V3.md` — 7 diagrammes |
| Historique V1/V2 | `mansart/HOW_TO_DEV_WITH_MISTRAL.md` — 949 lignes |
| Contrat des agents | `mansart/AGENTS.md` |
| Le garde-fou | `mansart/.vibe/hooks/guard-context.py` |
| Les scripts | `mansart/scripts/build.sh`, `verify.sh` |
| Commits de l'histoire | `c71f18a` V3, `c88b7aa` doc, `454862b` correctifs, `c96031a` le code mort |

Le code source du backend cité (mapping `thinking`, découpage des commandes,
ordre des vérifications de permission) est dans le paquet installé :
`~/.local/share/uv/tools/mistral-vibe/lib/python3.14/site-packages/vibe/`.
