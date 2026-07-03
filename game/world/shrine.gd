extends Area2D
## Santuário do Eco (bonfire): descansar cura, recarrega stamina e
## faz os inimigos renascerem.

var _player: Player

@onready var prompt: Label = $Prompt


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)
	prompt.visible = false


func _physics_process(_delta: float) -> void:
	# Input.is_action_just_pressed (e não _unhandled_input) porque os
	# controles de toque geram ações sintéticas sem InputEvent propagado.
	if _player and _player.is_alive() and Input.is_action_just_pressed("interact"):
		_rest()


func _on_body_entered(body: Node2D) -> void:
	if body is Player:
		_player = body
		prompt.visible = true


func _on_body_exited(body: Node2D) -> void:
	if body == _player:
		_player = null
		prompt.visible = false


func _rest() -> void:
	_player.respawn(_player.global_position)
	GameEvents.shrine_rested.emit()
	GameEvents.notify("Você descansou. Os Ecoados retornaram.")
