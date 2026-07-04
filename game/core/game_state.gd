extends Node
## Estado global da run: Ecos (moeda/XP), flags de progresso e o spawn
## de destino ao trocar de cena. Sobrevive a change_scene (autoload).

signal echoes_changed(amount: int)
signal inventory_changed
signal weapon_changed(level: int)
signal flasks_changed(current: int, max_value: int)

## Segundos de jogo para um ciclo completo de dia+noite.
const DAY_LENGTH := 300.0
## Tamanho (px) da célula do mapa da cartógrafa.
const MAP_CELL := 80.0

var echoes: int = 0
## Inventário: id do item -> quantidade (ver ItemDB).
var inventory: Dictionary = {}
## Nível de forja da lâmina (0 a 3) — ver forge.gd e player.gd.
var weapon_level: int = 0
## Frascos de Essência (cura limitada, recarregada no santuário).
var flasks: int = 3
var flasks_max: int = 3
## Hora do mundo em [0,1): 0 = amanhecer; noite em [0.5, 0.9).
var time_of_day := 0.15
## Mapa da cartógrafa: cena -> {"cols", "rows", "cells": Array[int]}.
var map_data: Dictionary = {}
## Flags de progresso permanente (ex.: "cripta_boss_derrotado").
var flags: Dictionary = {}
## Nome do Marker2D (em "Spawns/") onde a jogadora aparece na próxima cena.
## O valor especial "__shrine__" posiciona no santuário da cena.
var next_spawn := ""
## Cena do último santuário onde a jogadora descansou (ponto de load).
var last_shrine_scene := "res://world/village.tscn"


func _process(delta: float) -> void:
	time_of_day = fmod(time_of_day + delta / DAY_LENGTH, 1.0)


func reset() -> void:
	echoes = 0
	flags = {}
	inventory = {}
	weapon_level = 0
	flasks = 3
	flasks_max = 3
	time_of_day = 0.15
	map_data = {}
	next_spawn = ""
	last_shrine_scene = "res://world/village.tscn"
	echoes_changed.emit(echoes)
	inventory_changed.emit()
	flasks_changed.emit(flasks, flasks_max)


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


func upgrade_weapon() -> void:
	weapon_level += 1
	weapon_changed.emit(weapon_level)


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
