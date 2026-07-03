extends Area2D
## NPC: mostra o nome, um prompt de interação e abre diálogo via event bus.

@export var npc_name := "???"
@export var portrait: Texture2D
@export var dialog_lines: PackedStringArray = []

var _player: Player

@onready var prompt: Label = $Prompt
@onready var name_label: Label = $NameLabel
@onready var visual: Sprite2D = $Visual


func _ready() -> void:
	if portrait:
		visual.texture = portrait
	name_label.text = npc_name
	prompt.visible = false
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)


func _physics_process(_delta: float) -> void:
	# Input direto (e não _unhandled_input): os botões de toque geram
	# ações sintéticas sem InputEvent propagado.
	if _player and _player.is_alive() and not dialog_lines.is_empty() \
			and Input.is_action_just_pressed("interact"):
		GameEvents.dialog_requested.emit(npc_name, dialog_lines)


func _on_body_entered(body: Node2D) -> void:
	if body is Player:
		_player = body
		prompt.visible = true


func _on_body_exited(body: Node2D) -> void:
	if body == _player:
		_player = null
		prompt.visible = false
