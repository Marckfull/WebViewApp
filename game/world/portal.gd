extends Area2D
## Transição entre cenas: define em qual spawn a jogadora aparece no destino.

@export_file("*.tscn") var target_scene := ""
@export var target_spawn := "default"
@export var label_text := ""

@onready var label: Label = $Label


func _ready() -> void:
	if not label_text.is_empty():
		label.text = label_text
	body_entered.connect(_on_body_entered)


func _on_body_entered(body: Node2D) -> void:
	if body is Player and body.is_alive() and not target_scene.is_empty():
		GameState.next_spawn = target_spawn
		get_tree().change_scene_to_file.call_deferred(target_scene)
