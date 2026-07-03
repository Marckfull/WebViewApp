extends Node2D
## Arena de treino (Fase 0): valida o loop de combate souls-like.
## Gerencia morte da jogadora, drop de Ecos e respawn no santuário.

const ECHO_PICKUP := preload("res://world/echo_pickup.tscn")
const ARENA_SIZE := Vector2(960, 540)

var _death_drop: Node2D

@onready var player: Player = $Player
@onready var shrine: Area2D = $Shrine


func _ready() -> void:
	GameEvents.player_died.connect(_on_player_died)
	var cam: Camera2D = player.get_node("Camera2D")
	cam.limit_left = 0
	cam.limit_top = 0
	cam.limit_right = int(ARENA_SIZE.x)
	cam.limit_bottom = int(ARENA_SIZE.y)
	GameEvents.notify("Derrote os Ecoados. Recupere seus Ecos se cair.")


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
	player.respawn(shrine.global_position + Vector2(0, 28))
	GameEvents.shrine_rested.emit()
