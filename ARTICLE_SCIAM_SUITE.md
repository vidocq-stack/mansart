# J'ai ressorti mon rig de minage pour battre mon laptop. Il a perdu.

## La suite : trois cartes graphiques de compétition contre un ordinateur portable, et le résultat que je n'avais pas vu venir

*Par Yann Blazart, co-fondateur de Vidocq*

Dans l'épisode précédent, je vous racontais comment j'avais fait tourner une intelligence artificielle sur mon MacBook, coupé d'internet, pour lui faire écrire du vrai code. La conclusion tenait en une phrase : ça marche, à condition d'avoir la bonne machine, et cette machine coûte cher. Un lecteur, forcément, m'a posé la question qui fâche. « Cher, d'accord, mais est-ce que c'est la bonne dépense ? Tu ne ferais pas mieux avec de la vraie grosse artillerie ? »

La vraie grosse artillerie, justement, je l'avais sous la main. Et j'avais une vieille rancune à régler avec lui.

## Un fantôme dans le garage

Il faut que je vous parle d'une autre vie. Il y a quelques années, comme pas mal de gens qui savent visser deux composants ensemble, je me suis laissé tenter par le minage de cryptomonnaie. Le principe, pour ceux qui ont eu la sagesse de passer à côté : vous faites tourner des cartes graphiques jour et nuit pour résoudre des calculs inutiles, et en échange la machine vous recrache un peu de monnaie virtuelle. C'est bruyant, ça chauffe, ça mange de l'électricité comme un radiateur qu'on aurait oublié d'éteindre, et pendant un temps, ça rapportait.

J'ai donc monté ce qu'on appelle un rig : une structure ouverte, façon étagère à chaussures en métal, sur laquelle j'ai boulonné trois cartes graphiques haut de gamme. Trois RTX 3090. À l'époque, chacune coûtait le prix d'un très bon vélo, et à elles trois elles tiraient sur la prise l'équivalent d'un gros sèche-cheveux allumé en continu. Le genre d'objet qui fait grimper la facture et froncer les sourcils de la personne avec qui vous partagez le compteur.

L'histoire a bien fini, pour une fois. J'ai rentabilisé les cartes, j'ai revendu ce que je minais, et surtout j'ai eu le bon réflexe : j'ai tout arrêté juste avant que ça ne rapporte plus rien. Un timing dont je ne peux honnêtement pas m'attribuer le mérite, c'était plus de la chance que du flair. Depuis, le rig dort. Trois cartes de course qui prennent la poussière, un petit monument à une lubie passée.

Alors quand on m'a demandé si mon laptop n'était pas un caprice de bobo, j'ai souri. J'avais exactement de quoi trancher la question. J'ai soufflé sur la poussière, rebranché une des trois cartes sur une machine, et je me suis dit : on va bien voir qui est le caprice.

## L'intuition du garagiste

Posons le décor honnêtement, parce que mon pari de départ était limpide, et que vous l'auriez sûrement fait aussi.

D'un côté, un ordinateur portable. Fin, silencieux la plupart du temps, taillé pour tenir dans un sac. De l'autre, une carte graphique conçue pour une seule chose : avaler des montagnes de calculs à toute vitesse, sans jamais se fatiguer. Sur le papier, ce n'est même pas un match. C'est un poids plume contre un poids lourd. La carte a plus de muscle, plus de mémoire rapide, une architecture pensée pour la puissance brute depuis vingt ans. Le laptop, lui, a surtout l'avantage de tenir sur mes genoux.

Mon intuition, donc : la carte allait pulvériser le portable. Peut-être pas sur la facture d'électricité, mais sur la vitesse, sûrement. C'était couru d'avance.

Vous commencez à me connaître. Chaque fois que j'écris « c'était couru d'avance » dans ces pages, c'est qu'un banc d'essai s'apprête à me faire ravaler ma certitude.

## Premier round : la claque

J'ai donc chargé le même cerveau artificiel des deux côtés, et je les ai fait plancher sur le même travail, avec un chronomètre honnête. Pas le chiffre de la brochure, le vrai, celui qu'on relève soi-même.

Résultat : le laptop écrit environ cent quarante mots par seconde. La carte de course, dans les cent quinze. Le portable gagne. Nettement. J'ai recompté, changé le contexte, rechargé les modèles. Rien à faire, le petit Mac restait devant.

Comment un ordinateur de sac à dos peut-il battre une carte pensée pour la performance ? La réponse est dans la façon dont les modèles récents sont construits, et elle est jolie.

Le cerveau que j'utilise est ce qu'on appelle un modèle à experts. Imaginez une entreprise de trente-cinq mille personnes où, pour chaque tâche, on ne réveille que les trois mille spécialistes concernés. Le reste dort. C'est génial pour tenir sur une machine modeste, parce qu'à chaque instant, il n'y a qu'une petite équipe qui bosse.

Sauf que c'est aussi le talon d'Achille de ma grosse carte. Elle est bâtie pour faire travailler des dizaines de milliers d'ouvriers en même temps, à plein régime. Donnez-lui une tâche où seuls trois mille d'entre eux ont quelque chose à faire, et le reste de l'usine tourne à vide, moteurs allumés, personne aux machines. C'est une autoroute à huit voies sur laquelle roulent trois scooters. Toute cette largeur ne sert à rien si le trafic est maigre.

Le laptop, lui, n'a pas ce problème. Sa force n'est pas d'avoir huit voies, c'est d'avoir la mémoire et le calcul logés dans la même puce, à quelques millimètres l'un de l'autre, avec un chemin très court entre les deux. Pour une petite équipe qui travaille vite, ce chemin court compte davantage que la taille de l'autoroute. Et le logiciel qui pilote tout ça sur le Mac a été affûté au millimètre pour ce genre de modèle. La force brute s'est fait doubler par la finesse.

## Deuxième round : le gros bras qui n'était pas plus futé

À ce stade, j'aurais pu m'arrêter, mais j'entendais déjà l'objection. « Tu triches. Ton modèle à experts est fait pour les petites machines. Colle un vrai gros modèle sur la carte, un qui fait travailler tous ses neurones à chaque mot, et là elle va se réveiller. »

Objection légitime. Il se trouve que j'avais justement de quoi la tester : un autre modèle, plus classique, où il n'y a pas de spécialistes qui dorment. Chaque mot mobilise l'ensemble de ses neurones. Sur ce terrain, la grosse carte devrait enfin exprimer sa puissance, puisqu'on lui donne enfin du travail pour tout le monde.

Et là, deux surprises, l'une après l'autre.

La première : oui, la carte s'est réveillée, mais pas pour gagner. Ce gros modèle tourne à une cinquantaine de mots par seconde sur elle. Presque trois fois plus lent que le petit modèle à experts sur mon laptop. En faisant travailler tous ses neurones à chaque mot, il devient tellement lourd à faire tourner que même une carte de compétition peine. On a remplacé les trois scooters par un convoi exceptionnel : l'autoroute est enfin utile, mais le convoi avance au pas.

La seconde surprise, c'est celle qui m'a fait vraiment lever un sourcil. Je m'attendais à un compromis : ce gros modèle serait plus lent, d'accord, mais sûrement plus intelligent, non ? Plus de neurones au travail, plus de finesse dans les réponses. C'est l'intuition naturelle, et elle est fausse.

Je leur ai fait passer à tous les deux le même examen : des questions précises sur la norme que j'implémente, et des exercices où le modèle doit manier correctement ses outils. Le petit modèle à experts, sur mon laptop, a répondu juste à tout. Le gros modèle, sur la carte, a fait des fautes. Moins de bonnes réponses, pas plus. Le gros bras était non seulement plus lent, mais aussi un peu moins bon.

La leçon est presque contre-intuitive au point d'en être drôle : faire travailler plus de neurones ne rend pas un modèle plus malin. Ce qui le rend malin, c'est sa conception et son entraînement, pas le nombre de muscles qu'il contracte à chaque mot. Mon petit modèle est simplement mieux né que le gros. La taille impressionne, elle ne garantit rien.

## La facture, quand même

Il reste un détail que je n'ai pas le droit de passer sous silence, parce que c'est exactement celui qui vous rattrape à la fin du mois.

Pendant que mon laptop faisait tout ça en tirant l'équivalent d'une grosse ampoule sur la prise, la carte graphique, elle, avalait trois fois et demie plus d'électricité. Et je ne parle que d'une seule carte. Le rig complet, ses trois cartes ensemble, c'était un sèche-cheveux permanent, avec la chaleur et le bruit qui vont avec. Le genre de chaleur dont je me plaignais déjà dans le premier épisode, sauf multipliée.

Donc, récapitulons le match. Le laptop est plus rapide. Le laptop répond plus juste. Et le laptop consomme sept fois moins. Ce n'était pas censé être ça, le scénario. Le poids plume ne devait pas gagner aux points, à la puissance et sur la facture en même temps. Il l'a fait.

## Ce que le fantôme du garage m'a appris

Je vous vois venir, alors soyons précis, parce que je déteste les conclusions trop nettes. Je n'ai pas prouvé qu'une carte graphique est inutile pour l'intelligence artificielle. C'est faux, et il y a des cas très réels où elle reprend l'avantage : les modèles trop gros pour tenir dans mon Mac, ou le fait de servir beaucoup d'utilisateurs à la fois. Pour un labo, une entreprise, un service en ligne, ces cartes restent des bêtes de somme irremplaçables. Mon test dit une chose plus étroite, et donc plus honnête : pour mon usage précis, un développeur seul qui fait tourner un modèle moderne sur sa propre machine, mon laptop est le meilleur outil, et de loin.

Mais derrière l'anecdote, il y a quelque chose qui me plaît, et qui dépasse mon garage. Pendant des années, la course à la puissance informatique a été une course à la force brute. Plus de cartes, plus de watts, plus de chaleur. C'est cette logique qui a fait vivre le minage, et c'est cette logique que j'avais dans la tête en rebranchant mon rig. J'étais persuadé que le muscle gagnerait.

Or les modèles récents ont changé les règles sans prévenir. En apprenant à ne réveiller que les bons spécialistes au bon moment, ils récompensent désormais la finesse plutôt que la fournaise. Une puce sobre et bien conçue peut battre un mur de cartes voraces, non pas malgré sa modestie, mais grâce à elle. Le matériel qui gagne aujourd'hui n'est plus forcément celui qui consomme le plus. C'est un renversement discret, et je le trouve plutôt réjouissant, à une époque où on cherche des façons de calculer sans faire fondre la banquise.

Quant à mes trois RTX 3090, elles vont retourner dormir. Elles ont eu leur heure de gloire à hasher des calculs inutiles, puis leur petit baroud d'honneur contre un ordinateur portable. Elles ont perdu les deux fois, si on y réfléchit : la première parce que le minage s'est éteint, la seconde parce qu'un laptop les a doublées. Il y a une morale là-dedans sur les paris qu'on croit sûrs, mais je vais vous laisser la formuler vous-mêmes. Moi, je retourne coder, sur la petite machine sobre qui, décidément, n'avait pas besoin de mes gros bras.

---

*Yann Blazart développe la pile logicielle Vidocq. Comme dans le premier épisode, toutes les mesures de vitesse et de qualité citées ici ont été relevées sur ses propres machines et consignées ; il les fournira à quiconque doute, ce qui reste la moindre des choses quand on parle d'intelligence artificielle. Et, transparence oblige : cet article a été écrit avec l'aide d'une IA, mais les cartes graphiques, la facture d'électricité et les intuitions démenties, elles, étaient bien réelles.*
