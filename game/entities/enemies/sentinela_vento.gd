extends EnemyBase
## Sentinela do Vento: inimigo à distância da Torre. Mantém-se afastada
## e dispara rajadas telegrafadas (não corpo-a-corpo).

const GUST := preload("res://world/gust_projectile.tscn")


func _on_attack_start() -> void:
	var gust := GUST.instantiate()
	gust.global_position = global_position
	gust.direction = _attack_dir
	get_parent().add_child(gust)
	AudioManager.play_sfx("swing")
