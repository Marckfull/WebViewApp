extends Node
## GameProgress — persiste a progressão via eventos (autoload, §3.6).
##
## Centraliza: preencher o bestiário ao derrotar inimigos, registrar bosses
## derrotados e disparar autosave nos momentos-chave (mobile: o jogador é
## interrompido o tempo todo — não se deve depender só do descanso).

func _ready() -> void:
	GameEvents.enemy_defeated.connect(_on_enemy_defeated)
	GameEvents.boss_defeated.connect(_on_boss_defeated)
	GameEvents.melody_played.connect(_on_melody_played)

func _on_enemy_defeated(enemy_id: StringName, _pos: Vector2) -> void:
	var entries: Array = SaveManager.state["bestiary"]
	if Bestiary.record(enemy_id, entries):
		GameEvents.bestiary_entry_unlocked.emit(enemy_id)

func _on_boss_defeated(boss_id: StringName) -> void:
	var defeated: Array = SaveManager.state["world"]["bosses_defeated"]
	if not defeated.has(String(boss_id)):
		defeated.append(String(boss_id))
	SaveManager.save_game()  # autosave: marco importante

func _on_melody_played(_melody_id: StringName) -> void:
	# Aprender/usar a Ocarina é progressão — vale um autosave.
	SaveManager.save_game()
