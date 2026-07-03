class_name Level
extends Node2D
## Base de toda cena jogável: posiciona a jogadora no spawn de entrada,
## limita a câmera à arena e gerencia o loop souls-like de
## morte → drop de Ecos → respawn no santuário.

const ECHO_PICKUP := preload("res://world/echo_pickup.tscn")

@export var arena_size := Vector2(960, 540)
@export var intro_message := ""
@export var music := "village"

var _death_drop: Node2D

@onready var player: Player = $Player


func _ready() -> void:
	GameEvents.player_died.connect(_on_player_died)
	var cam: Camera2D = player.get_node("Camera2D")
	cam.limit_left = 0
	cam.limit_top = 0
	cam.limit_right = int(arena_size.x)
	cam.limit_bottom = int(arena_size.y)
	_place_player_at_spawn()
	AudioManager.play_level_music(music)
	if not intro_message.is_empty():
		GameEvents.notify(intro_message)


func _place_player_at_spawn() -> void:
	if GameState.next_spawn.is_empty():
		return
	if GameState.next_spawn == "__shrine__":
		# Carregou um save: acorda ao lado do santuário da cena.
		GameState.next_spawn = ""
		var shrine := get_node_or_null("Shrine")
		if shrine:
			player.global_position = shrine.global_position + Vector2(0, 28)
			player.get_node("Camera2D").reset_smoothing()
		return
	var spawn := get_node_or_null("Spawns/" + GameState.next_spawn)
	GameState.next_spawn = ""
	if spawn:
		player.global_position = spawn.global_position
		player.get_node("Camera2D").reset_smoothing()


func _respawn_point() -> Vector2:
	var shrine := get_node_or_null("Shrine")
	if shrine:
		return shrine.global_position + Vector2(0, 28)
	return player.global_position


func _on_player_died(death_position: Vector2) -> void:
	var lost := GameState.lose_all_echoes()
	if lost > 0:
		# Regra souls-like: um novo drop substitui o anterior não coletado.
		if is_instance_valid(_death_drop):
			_death_drop.queue_free()
		_death_drop = ECHO_PICKUP.instantiate()
		_death_drop.amount = lost
		_death_drop.position = death_position
		# Deferido: o sinal de morte chega durante o flush de física.
		add_child.call_deferred(_death_drop)
	GameEvents.notify("VOCÊ SE PERDEU NO SILÊNCIO")
	await get_tree().create_timer(2.0).timeout
	player.respawn(_respawn_point())
	GameEvents.shrine_rested.emit()
