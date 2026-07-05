extends Node
## Estado global da run: Ecos (moeda/XP), flags de progresso e o spawn
## de destino ao trocar de cena. Sobrevive a change_scene (autoload).

signal echoes_changed(amount: int)
signal inventory_changed
signal weapon_changed(level: int)
signal flasks_changed(current: int, max_value: int)
signal attributes_changed
signal weapon_equipped(id: String)

enum Difficulty { BALADA, CANCAO, REQUIEM }

## Segundos de jogo para um ciclo completo de dia+noite.
const DAY_LENGTH := 300.0
## Tamanho (px) da célula do mapa da cartógrafa.
const MAP_CELL := 80.0

## Flags de progresso zeradas ao entrar em New Game+ (bosses e história);
## itens coletados e quests são mantidos para não duplicar recompensas.
const NG_RESET_FLAGS := [
	"cripta_boss_derrotado", "floresta_boss_derrotado", "forja_boss_derrotado",
	"torre_boss_derrotado", "necropole_boss_derrotado", "selene_derrotada",
	"jogo_concluido", "final_silenciar", "final_completar", "final_cancao",
]

var echoes: int = 0
## Inventário: id do item -> quantidade (ver ItemDB).
var inventory: Dictionary = {}
## Nível de forja da lâmina (0 a 3) — ver forge.gd e player.gd.
var weapon_level: int = 0
## Arma empunhada (ver WeaponDB).
var equipped_weapon := "espada"
## Frascos de Essência (cura limitada, recarregada no santuário).
var flasks: int = 3
var flasks_max: int = 3
## Atributos comprados com Ecos no santuário (level up souls-like).
var attributes: Dictionary = {"vit": 0, "fol": 0, "forca": 0}
## Hora do mundo em [0,1): 0 = amanhecer; noite em [0.5, 0.9).
var time_of_day := 0.15
## Mapa da cartógrafa: cena -> {"cols", "rows", "cells": Array[int]}.
var map_data: Dictionary = {}
## Bestiário: id da criatura -> quantidade derrotada (ver BestiaryDB).
var bestiary: Dictionary = {}
## Flags de progresso permanente (ex.: "cripta_boss_derrotado").
var flags: Dictionary = {}
## Nome do Marker2D (em "Spawns/") onde a jogadora aparece na próxima cena.
## O valor especial "__shrine__" posiciona no santuário da cena.
var next_spawn := ""
## Cena do último santuário onde a jogadora descansou (ponto de load).
var last_shrine_scene := "res://world/village.tscn"
## Modo de dificuldade escolhido na criação da run.
var difficulty: int = Difficulty.CANCAO
## Ciclo de New Game+ (0 = primeira jogada); escala os inimigos.
var ng_cycle: int = 0
## Cutscene pendente (transitório): falas e cena para retornar depois.
var pending_cutscene: PackedStringArray = []
var cutscene_return := ""


## Os 4 Santuários do Eco foram restaurados? (abre o Coração Mudo.)
func act2_complete() -> bool:
	return flags.get("floresta_boss_derrotado", false) \
			and flags.get("forja_boss_derrotado", false) \
			and flags.get("torre_boss_derrotado", false) \
			and flags.get("necropole_boss_derrotado", false)


func _process(delta: float) -> void:
	time_of_day = fmod(time_of_day + delta / DAY_LENGTH, 1.0)


# --- Multiplicadores de dificuldade + New Game+ (ponto central) ---

## Dano que a jogadora RECEBE. Requiem bate mais forte; NG+ agrava.
func enemy_damage_mult() -> float:
	var base := [0.7, 1.0, 1.3][difficulty]
	return base * (1.0 + 0.2 * ng_cycle)


## Vida dos inimigos — só escala no New Game+.
func enemy_health_mult() -> float:
	return 1.0 + 0.4 * ng_cycle


## Ecos ganhos — mais generoso a cada ciclo de NG+.
func echo_mult() -> float:
	return 1.0 + 0.5 * ng_cycle


## Janela ativa do parry (segundos): generosa na Balada, apertada no Requiem.
func parry_window() -> float:
	return [0.28, 0.18, 0.12][difficulty]


func flasks_for_difficulty() -> int:
	return [5, 3, 2][difficulty]


## Na Balada, a jogadora não perde os Ecos ao morrer.
func keeps_echoes_on_death() -> bool:
	return difficulty == Difficulty.BALADA


func difficulty_name() -> String:
	return ["Balada", "Canção", "Requiem"][difficulty]


## Começa uma volta de New Game+: mantém progressão (itens, atributos,
## forja, frascos, Ecos), zera os avanços de história e agrava os inimigos.
func start_ng_plus() -> void:
	ng_cycle += 1
	for flag in NG_RESET_FLAGS:
		flags.erase(flag)
	flasks = flasks_max
	time_of_day = 0.15
	map_data = {}
	next_spawn = ""
	last_shrine_scene = "res://world/village.tscn"
	echoes_changed.emit(echoes)
	inventory_changed.emit()
	flasks_changed.emit(flasks, flasks_max)
	attributes_changed.emit()


func reset() -> void:
	echoes = 0
	flags = {}
	inventory = {}
	weapon_level = 0
	equipped_weapon = "espada"
	flasks = 3
	flasks_max = 3
	attributes = {"vit": 0, "fol": 0, "forca": 0}
	time_of_day = 0.15
	map_data = {}
	bestiary = {}
	next_spawn = ""
	last_shrine_scene = "res://world/village.tscn"
	difficulty = Difficulty.CANCAO
	ng_cycle = 0
	echoes_changed.emit(echoes)
	inventory_changed.emit()
	flasks_changed.emit(flasks, flasks_max)
	attributes_changed.emit()


func is_night() -> bool:
	return time_of_day >= 0.5 and time_of_day < 0.9


## Cor da luz ambiente para CanvasModulate (cenas externas).
func daylight_color() -> Color:
	var day := Color.WHITE
	var dusk := Color(1.0, 0.72, 0.55)
	var night := Color(0.45, 0.5, 0.78)
	var t := time_of_day
	if t < 0.40:
		return day
	if t < 0.45:
		return day.lerp(dusk, (t - 0.40) / 0.05)
	if t < 0.50:
		return dusk.lerp(night, (t - 0.45) / 0.05)
	if t < 0.90:
		return night
	if t < 0.95:
		return night.lerp(dusk, (t - 0.90) / 0.05)
	return dusk.lerp(day, (t - 0.95) / 0.05)


func use_flask() -> bool:
	if flasks <= 0:
		return false
	flasks -= 1
	flasks_changed.emit(flasks, flasks_max)
	return true


func refill_flasks() -> void:
	flasks = flasks_max
	flasks_changed.emit(flasks, flasks_max)


## Marca a célula do mapa onde a jogadora está (mecânica da cartógrafa).
func mark_visited(scene: String, arena: Vector2, pos: Vector2) -> void:
	if scene.is_empty():
		return
	if not map_data.has(scene):
		map_data[scene] = {
			"cols": int(ceil(arena.x / MAP_CELL)),
			"rows": int(ceil(arena.y / MAP_CELL)),
			"cells": [],
		}
	var entry: Dictionary = map_data[scene]
	var cols: int = entry["cols"]
	var cell := clampi(int(pos.x / MAP_CELL), 0, cols - 1) \
			+ clampi(int(pos.y / MAP_CELL), 0, int(entry["rows"]) - 1) * cols
	if not entry["cells"].has(cell):
		entry["cells"].append(cell)


func add_item(id: String, amount: int = 1) -> void:
	inventory[id] = int(inventory.get(id, 0)) + amount
	inventory_changed.emit()


func remove_item(id: String, amount: int = 1) -> void:
	var left := int(inventory.get(id, 0)) - amount
	if left > 0:
		inventory[id] = left
	else:
		inventory.erase(id)
	inventory_changed.emit()


func has_item(id: String) -> bool:
	return int(inventory.get(id, 0)) > 0


func item_count(id: String) -> int:
	return int(inventory.get(id, 0))


## Registra uma criatura derrotada no bestiário.
func record_kill(id: String) -> void:
	if id.is_empty():
		return
	bestiary[id] = int(bestiary.get(id, 0)) + 1


## Ids de Memórias Perdidas já reunidas.
func memories_collected() -> Array:
	var found: Array = []
	for id in inventory:
		if str(id).begins_with("memoria_"):
			found.append(id)
	return found


func upgrade_weapon() -> void:
	weapon_level += 1
	weapon_changed.emit(weapon_level)


func equip_weapon(id: String) -> void:
	if WeaponDB.DB.has(id):
		equipped_weapon = id
		weapon_equipped.emit(id)


func weapon() -> Dictionary:
	return WeaponDB.get_weapon(equipped_weapon)


func spend_echoes(amount: int) -> bool:
	if echoes < amount:
		return false
	echoes -= amount
	echoes_changed.emit(echoes)
	return true


## Custo do próximo nível de atributo (sobe com o total já comprado).
func attribute_cost() -> int:
	var total := 0
	for key in attributes:
		total += int(attributes[key])
	return 100 + total * 50


func raise_attribute(key: String) -> bool:
	if not attributes.has(key) or not spend_echoes(attribute_cost()):
		return false
	attributes[key] = int(attributes[key]) + 1
	attributes_changed.emit()
	return true


func add_echoes(amount: int) -> void:
	echoes += amount
	echoes_changed.emit(echoes)


## Chamado quando a jogadora morre: perde tudo e retorna o valor perdido
## para ser dropado no local da morte (loop souls-like).
func lose_all_echoes() -> int:
	var lost := echoes
	echoes = 0
	echoes_changed.emit(echoes)
	return lost
