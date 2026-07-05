extends NpcBase
## Mestra Odara: quando a lâmina de Aria atinge a forja máxima, a ferreira
## reconhece a maestria e entrega uma Memória Perdida.

const MAX_WEAPON := 3

const LINES_DEFAULT := [
	"Aria? Você voltou... A vila não é a mesma desde que o Silêncio passou.",
	"Levaram a pequena Lys para o norte, na direção da Cripta das Guardiãs.",
	"Minha forja está fria. Mas se me trouxer minério das ruínas, ainda sei "
			+ "fazer uma lâmina cantar.",
]
const LINES_MEMORY := [
	"Essa lâmina... ela canta como as antigas. Você aprendeu, menina.",
	"Tome. Uma lembrança que eu guardava para quem merecesse a forja.",
]
const LINES_DONE := [
	"A forja está viva de novo, graças a você. Sinta o peso: é o peso certo.",
]


func _interact() -> void:
	if GameState.weapon_level >= MAX_WEAPON:
		if not GameState.has_item("memoria_odara"):
			GameState.add_item("memoria_odara")
			AudioManager.play_sfx("pickup")
			GameEvents.dialog_requested.emit(npc_name, LINES_MEMORY)
			GameEvents.notify("Recebeu: Memória Perdida — a Mestra")
		else:
			GameEvents.dialog_requested.emit(npc_name, LINES_DONE)
		return
	GameEvents.dialog_requested.emit(npc_name, LINES_DEFAULT)
