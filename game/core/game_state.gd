extends Node
## Estado global da run: Ecos (moeda/XP), flags de progresso e o spawn
## de destino ao trocar de cena. Sobrevive a change_scene (autoload).

signal echoes_changed(amount: int)

var echoes: int = 0
## Flags de progresso permanente (ex.: "cripta_boss_derrotado").
var flags: Dictionary = {}
## Nome do Marker2D (em "Spawns/") onde a jogadora aparece na próxima cena.
var next_spawn := ""


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
