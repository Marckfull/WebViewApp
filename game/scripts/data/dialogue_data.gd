class_name DialogueData
extends Resource
## DialogueData — diálogo data-driven (§3.5, §6.2). Designers criam .tres sem tocar
## código. Um falante por diálogo; **ramificação por escolhas** via arrays paralelos
## (choice_texts + choice_next): ao fim das linhas, cada escolha leva a outro
## DialogueData (ou encerra, se o próximo for nulo).

@export var id: StringName
@export var speaker: String = ""
@export_multiline var lines: Array[String] = []
## Ramificação (§3.5): textos das opções e os diálogos-destino (paralelos). Um
## destino nulo encerra a conversa. Vazio = diálogo linear (comportamento antigo).
@export var choice_texts: Array[String] = []
@export var choice_next: Array[DialogueData] = []

func has_choices() -> bool:
	return not choice_texts.is_empty()

## Diálogo-destino da escolha `i` (null = encerra).
func next_for(i: int) -> DialogueData:
	if i >= 0 and i < choice_next.size():
		return choice_next[i]
	return null
