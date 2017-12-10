# TTV-Lab2

## Übersicht
### Strategie
Unsere Anwendung ist hinsichtlich der Strategie modular aufgebaut, sodass unterschiedliche Strategien entwickelt und vor einem Spiel ausgetauscht werden können. Wir unterscheidern hierbei zwischen zwei Strategiearten die wir umsetzen müssen. Zum einen eine Strategie zum Platzieren der Schiffe und zum anderen eine für die Auswahl eines Schiffes auf das geschossen werden soll. 

Um dies umzusetzen haben wir eine abstrakte Klasse "Strategy" entworfen, welches die beiden oben genannten Strategiarten durch zwei Methoden definiert. Strategien die diese Klasse implementieren müssen somit auch die Strategien umsetzen.

#### StrategyOne
**Übersicht über Netzwerk** Für die Auswahl eines sinnvollen Ziels, welches wir angreifen wollen, müssen wir die genaue Aufteilung des Netzwerkes kennen. D.h. wir müssen wissen, welcher Knoten für welches Intervall zuständig ist. Über unseren Successor können wir uns schrittweise eine Aufteilung über das gesamte Netzwerk verschaffen. Wir werden bei unserem Successor anfangen und dessen ID mit 1 addieren und das Netzwerk danach fragen, wer für diese ID zuständig ist. Das Ergebnis wird derjeniege Knoten sein, der auf unseren Successor folgt. Die NodeID und das zugehörige Interval speichern wir ab. Anschließend verfahren wir mit der ID dieses Knotens weiter, solange bis wir bei uns selbst angekommen sind. 

**Schiffauswahl (Ziel)**
 Durch die empfangenen Broadcasts sind wir in der Lage in unserer Anwendung alle erfolgreichen und missglückten Versuche ein Schiff eines Spielers zu versenken zu zählen. Diese Information machen wir uns zunutze, indem wir immer den Spieler wählen der bereits die meisten Schiffe verloren hat. Gibt es mehrere suchen wir uns einen zufällig von diesen aus. Gibt es keinen einzigen solchen Spieler wählen wir einen Spieler aus bei dem die meisten missglückten Versuche ein Schiff zu versenken verzeichnet wurden. Gibt es mehrere, wählen wir einen zufällig von diesen aus. Gibt es aber keinen solchen Spieler auf den eine der vier vorher genannten Bedingungen zutreffen, suchen wir einen zufällig aus. 

 Dadurch kennen wir bereits die NodeID unseres nächsten Ziels und können durch unser vorheriges "aushorchen" des Netzwerkes genau feststellen, welches Interval unser nächstes Ziel "verwaltet". Zunächst müssen wir dieses Intervall allerdings in 100 gleichgroße Intervalle einteilen in denen die 10 Schiffe möglicherweise platziert wurden. Erst mit dieser weiteren Einteilung können wir feststellen in welchem Interval dieses Spielers bereits Schiffe versenkt wurden oder missglückte Schüsse stattfanden. Kennen wir diejenigen Intervalle auf denen noch kein Schuss abgefeuert wurde, suchen wir uns ein zufälliges davon aus.

**Eigene Schiffe platzieren** 