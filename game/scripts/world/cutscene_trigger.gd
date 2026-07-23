extends Area2D
## CutsceneTrigger — dispara uma cutscene quando Aria entra na área (§2). "once"
## por padrão: toca uma vez (persistido como vista). Detecta o corpo do Player.

@export var cutscene: CutsceneData
@export var once: bool = true

func _ready() -> void:
	body_entered.connect(_on_body_entered)

func _on_body_entered(body: Node) -> void:
	if not body.is_in_group("player"):
		return
	if once:
		CutsceneManager.play_once(cutscene)
	else:
		CutsceneManager.play(cutscene)
