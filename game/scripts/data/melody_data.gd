class_name MelodyData
extends Resource
## MelodyData — melodia da Ocarina data-driven (§3.1, §6). 8 melodias planejadas.
## `notes` é a sequência de índices (0..4) no mini-teclado de 5 notas de OoT.

enum Effect { NONE, CALM_ENEMIES, DAY_NIGHT, WARP }

@export var id: StringName
@export var display_name: String = "Melodia"
@export var notes: Array[int] = []
@export var effect: Effect = Effect.NONE
