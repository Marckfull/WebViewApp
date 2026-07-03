extends Node
## Estado global da run: Ecos (moeda/XP), flags de progresso e o spawn
## de destino ao trocar de cena. Sobrevive a change_scene (autoload).

signal echoes_changed(amount: int)
signal inventory_changed

var echoes: int = 0
## Inventário: id do item -> quantidade (ver ItemDB).
var inventory: Dictionary = {}
## Flags de progresso permanente (ex.: "cripta_boss_derrotado").
var flags: Dictionary = {}
## Nome do Marker2D (em "Spawns/") onde a jogadora aparece na próxima cena.
## O valor especial "__shrine__" posiciona no santuário da cena.
var next_spawn := ""
## Cena do último santuário onde a jogadora descansou (ponto de load).
var last_shrine_scene := "res://world/village.tscn"


func reset() -> void:
	echoes = 0
	flags = {}
	inventory = {}
	next_spawn = ""
	last_shrine_scene = "res://world/village.tscn"
	echoes_changed.emit(echoes)
	inventory_changed.emit()


func add_item(id: String, amount: int = 1) -> void:
	inventory[id] = int(inventory.get(id, 0)) + amount
	inventory_changed.emit()


func has_item(id: String) -> bool:
	return int(inventory.get(id, 0)) > 0


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
