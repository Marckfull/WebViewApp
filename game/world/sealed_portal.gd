extends Area2D
## Portal selado: só leva ao destino quando todas as flags exigidas
## estão marcadas (os 4 Santuários do Eco restaurados). Antes disso,
## avisa que o caminho continua selado.

@export_file("*.tscn") var target_scene := ""
@export var target_spawn := "default"
@export var label_text := ""
@export var required_flags: PackedStringArray = []

@onready var label: Label = $Label


func _ready() -> void:
	if not label_text.is_empty():
		label.text = label_text
	body_entered.connect(_on_body_entered)


func _is_open() -> bool:
	for flag in required_flags:
		if not GameState.flags.get(flag, false):
			return false
	return true


func _on_body_entered(body: Node2D) -> void:
	if not (body is Player and body.is_alive()) or target_scene.is_empty():
		return
	if not _is_open():
		var faltam := 0
		for flag in required_flags:
			if not GameState.flags.get(flag, false):
				faltam += 1
		GameEvents.notify("O Coração Mudo está selado. Faltam %d Ecos." % faltam)
		return
	GameState.next_spawn = target_spawn
	get_tree().change_scene_to_file.call_deferred(target_scene)
