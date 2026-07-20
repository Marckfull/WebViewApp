class_name HealthComponent
extends Node
## Vida + postura (poise). Golpes pesados quebram a postura -> atordoamento ->
## finalização (§3.2). Reutilizável por Aria, ~35 inimigos e 12 bosses.

signal died
signal poise_broken
signal health_changed(current: float, maximum: float)  ## local (bosses, barras próprias)

@export var max_health: float = 100.0
@export var max_poise: float = 50.0
@export var poise_regen_per_second: float = 20.0
@export var is_player: bool = false

var health: float
var poise: float
var _stunned: bool = false

func _ready() -> void:
	health = max_health
	poise = max_poise

func _process(delta: float) -> void:
	if poise < max_poise and not _stunned:
		poise = minf(poise + poise_regen_per_second * delta, max_poise)

## Aplica dano de vida e de postura. poise_damage alto = golpe pesado.
func take_damage(amount: float, poise_damage: float = 0.0) -> void:
	if health <= 0.0:
		return
	health = maxf(health - amount, 0.0)
	health_changed.emit(health, max_health)
	if is_player:
		GameEvents.health_changed.emit(health, max_health)
	if poise_damage > 0.0 and not _stunned:
		poise = maxf(poise - poise_damage, 0.0)
		if poise <= 0.0:
			_stunned = true
			poise_broken.emit()
			GameEvents.poise_broken.emit(get_parent())
	if health <= 0.0:
		died.emit()

func recover_poise() -> void:
	_stunned = false
	poise = max_poise

func is_stunned() -> bool:
	return _stunned

func heal(amount: float) -> void:
	health = minf(health + amount, max_health)
	if is_player:
		GameEvents.health_changed.emit(health, max_health)

## Ajusta a vida máxima (ex.: subir Vitalidade num santuário, §3.3).
## `refill` recompleta a vida; senão preserva a proporção atual.
func set_max_health(value: float, refill: bool = true) -> void:
	max_health = maxf(value, 1.0)
	health = max_health if refill else minf(health, max_health)
	health_changed.emit(health, max_health)
	if is_player:
		GameEvents.health_changed.emit(health, max_health)
