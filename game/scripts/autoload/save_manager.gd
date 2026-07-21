extends Node
## SaveManager — save JSON versionado, escrita atômica, autosave (autoload).
##
## Implementa o §6.2/§3.6 do GDD: save-anywhere suspenso, 3 slots manuais +
## autosave, escrita atômica (grava em .tmp e renomeia) para não corromper o
## save se o app for morto pelo Android no meio da gravação — cenário comum em
## mobile onde o jogador é interrompido o tempo todo.

const SAVE_DIR := "user://saves"
const SAVE_VERSION := 1
const AUTOSAVE_SLOT := 0

## Estado do jogo em memória. Data-driven: qualquer sistema lê/escreve aqui e o
## SaveManager só serializa.
var state: Dictionary = _default_state()

## Estado inicial de um jogo novo. Função (não literal compartilhado) para que
## cada Novo Jogo receba uma cópia limpa, sem vazar dados entre partidas.
func _default_state() -> Dictionary:
	return {
		"version": SAVE_VERSION,
		"aria": { "hp": 100.0, "max_hp": 100.0, "ecos": 0, "position": [0.0, 0.0] },
		"attributes": { "vitalidade": 1, "stamina": 1, "forca": 1, "destreza": 1, "harmonia": 1 },
		"world": {
			"shrines": [], "areas_discovered": [], "melodies": [], "day_time": 0.0,
			"bosses_defeated": [],  ## bosses não renascem após carregar
			"eco_drop": {},         ## drop de Ecos persistido entre sessões (§3.2/§3.6)
			"map": {},              ## fog-of-war do mini-mapa por sala (§3.5)
			"walls_broken": [],     ## paredes rachadas já estilhaçadas (Bomba, §3.3)
		},
		"bestiary": [],
		"consumables": {}, ## poções e afins: id -> quantidade (§3.5)
		"resources": {},   ## recursos de craft: minério, madeira... id -> quantidade (§3.5)
		"weapon_levels": {}, ## nível de upgrade por arma na forja (§3.3)
		"quests": {},      ## side quests: id -> { status, progress } (§3.5)
		"items": [],     ## itens-chave de dungeon: gancho, bomba, lente... (§3.3)
		"memories": [],  ## 12 Memórias Perdidas -> final secreto (§2, §3.3)
		"playtime": 0.0,
		"settings": { "telemetry": false },  ## telemetria opt-in de playtest (§6.3)
	}

## Reinicia para um jogo novo (usado pelo menu "Novo Jogo").
func reset_state() -> void:
	state = _default_state()

func _ready() -> void:
	DirAccess.make_dir_recursive_absolute(SAVE_DIR)

func _slot_path(slot: int) -> String:
	return "%s/slot_%d.json" % [SAVE_DIR, slot]

## Gravação atômica: escreve em .tmp, depois renomeia sobre o alvo.
func save_game(slot: int = AUTOSAVE_SLOT) -> bool:
	state["version"] = SAVE_VERSION
	var path := _slot_path(slot)
	var tmp := path + ".tmp"
	var f := FileAccess.open(tmp, FileAccess.WRITE)
	if f == null:
		push_error("SaveManager: não foi possível abrir %s (err %d)" % [tmp, FileAccess.get_open_error()])
		return false
	f.store_string(JSON.stringify(state, "\t"))
	f.close()
	var dir := DirAccess.open(SAVE_DIR)
	if dir == null:
		return false
	# rename() sobrescreve o destino — troca atômica no mesmo volume.
	var err := dir.rename(tmp.get_file(), path.get_file())
	if err != OK:
		push_error("SaveManager: falha ao renomear save (err %d)" % err)
		return false
	GameEvents.game_saved.emit(slot)
	return true

func load_game(slot: int = AUTOSAVE_SLOT) -> bool:
	var path := _slot_path(slot)
	if not FileAccess.file_exists(path):
		return false
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return false
	var parsed: Variant = JSON.parse_string(f.get_as_text())
	f.close()
	if typeof(parsed) != TYPE_DICTIONARY:
		push_error("SaveManager: save corrompido em %s" % path)
		return false
	state = _migrate(parsed)
	GameEvents.game_loaded.emit(slot)
	return true

func has_save(slot: int) -> bool:
	return FileAccess.file_exists(_slot_path(slot))

## Migração entre versões de save (§6.2: save versionado + robusto). Mescla os
## defaults atuais para que saves antigos ganhem as chaves novas sem quebrar.
func _migrate(data: Dictionary) -> Dictionary:
	data = SaveMigration.merge_defaults(data, _default_state())
	data["version"] = SAVE_VERSION
	return data
