extends Area2D
## TutorialHint — dica diegética (§4, HUD/UI). Uma aura silenciosa que revela um
## texto curto quando Aria se aproxima e o esconde quando ela se afasta. Sem menu
## e sem pausa: ensina o controle no próprio mundo, no ritmo do jogador.

@export_multiline var text: String = ""
@onready var _label: Label = $Label

func _ready() -> void:
	_label.text = text
	_label.visible = false
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)

func _on_body_entered(body: Node) -> void:
	if body.is_in_group("player"):
		_label.visible = true

func _on_body_exited(body: Node) -> void:
	if body.is_in_group("player"):
		_label.visible = false
