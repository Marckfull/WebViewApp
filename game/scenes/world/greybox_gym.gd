extends Node2D
## Greybox Gym — cena de tuning de combate isolado (§6.3 "gym de combate").
##
## Fase 0 / gate do GDD: mover, atacar, esquivar (i-frames), defender/parry,
## lock-on, stamina, morte com drop de Ecos. Reinicia ao morrer para iteração
## rápida. É aqui que se responde "o combate é divertido?".

@export var enemy_scene: PackedScene = preload("res://scenes/enemies/dummy_enemy.tscn")

func _ready() -> void:
	GameEvents.player_died.connect(_on_player_died)
	GameEvents.parry_success.connect(func(t): print("PARRY! -> ", t))

func _on_player_died(_pos: Vector2, ecos: int) -> void:
	print("Aria caiu. Ecos deixados no local: ", ecos)
	await get_tree().create_timer(1.5).timeout
	get_tree().reload_current_scene()
