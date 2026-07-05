extends NpcBase
## Sela e a side quest "O canteiro da mamãe": traga 5 Ervas-lunares
## e receba o Talismã de Sela (ataques/esquivas gastam 20% menos vigor).

const HERBS_NEEDED := 5

const LINES_START := [
	"Os monstros... eles eram pessoas. Eu reconheci o padeiro entre eles.",
	"Minha mãe tinha um canteiro de Ervas-lunares. O Silêncio o engoliu.",
	"Se você me trouxer 5 Ervas-lunares, devolvo algo que era dela. Por favor.",
]
const LINES_WAITING := [
	"Achou as ervas? Preciso de 5 Ervas-lunares. Elas brilham no escuro...",
	"Elas renascem onde a terra ainda lembra. Descanse e procure de novo.",
]
const LINES_COMPLETE := [
	"Você... encontrou. Elas ainda têm o cheiro do canteiro dela.",
	"Tome. O talismã da minha mãe. Ela dizia que ele deixava o corpo leve.",
	"Obrigada, Aria. Agora uma parte dela continua andando por Lirael.",
]
const LINES_MEMORY := [
	"Espere — enquanto arrumava o canteiro, achei mais uma coisa dela.",
	"Uma lembrança. Guarde você; meu coração já está cheio do que lembro.",
]
const LINES_DONE := [
	"O talismã combina com você. Mamãe teria gostado de te ver lutar.",
]


func _interact() -> void:
	if GameState.flags.get("quest_sela_concluida", false):
		if not GameState.has_item("memoria_sela"):
			GameState.add_item("memoria_sela")
			AudioManager.play_sfx("pickup")
			GameEvents.dialog_requested.emit(npc_name, LINES_MEMORY)
			GameEvents.notify("Recebeu: Memória Perdida — o Canteiro")
		else:
			GameEvents.dialog_requested.emit(npc_name, LINES_DONE)
		return
	if not GameState.flags.get("quest_sela_iniciada", false):
		GameState.flags["quest_sela_iniciada"] = true
		GameEvents.dialog_requested.emit(npc_name, LINES_START)
		return
	if GameState.item_count("erva_lunar") >= HERBS_NEEDED:
		GameState.remove_item("erva_lunar", HERBS_NEEDED)
		GameState.add_item("talisma_sela")
		GameState.flags["quest_sela_concluida"] = true
		AudioManager.play_sfx("victory")
		GameEvents.dialog_requested.emit(npc_name, LINES_COMPLETE)
		GameEvents.notify("Recebeu: Talismã de Sela")
	else:
		GameEvents.dialog_requested.emit(npc_name, LINES_WAITING)
