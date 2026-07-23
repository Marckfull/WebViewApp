extends GameWorld
## Encruzilhada dos Ecos — hub do Ato 2 (§2). Herda o GameWorld e dispara a
## REVIRAVOLTA do fim do Ato 2: quando Aria volta ao hub com os 4 Santuários já
## restaurados (os 4 bosses de dungeon derrotados), descobre que restaurá-los
## afinou Lys para ser a próxima Guardiã. Toca uma única vez.

const REVIRAVOLTA := "res://data/cutscenes/reviravolta_ato2.tres"

func _ready() -> void:
	super._ready()
	# Adiado: o CutsceneManager/HUD precisam estar prontos antes do overlay.
	_maybe_reviravolta.call_deferred()

func _maybe_reviravolta() -> void:
	var defeated: Array = SaveManager.state["world"].get("bosses_defeated", [])
	if not ActProgress.all_cleared(defeated):
		return
	var c := load(REVIRAVOLTA) as CutsceneData
	if c:
		CutsceneManager.play_once(c)
