class_name EquipmentData
extends Resource
## Equipamento data-driven (§3.3): armaduras e amuletos. Cada peça vai num slot
## (armadura ou amuleto) e soma bônus passivos. Designers criam .tres em
## data/equipment/ sem tocar código.

enum Slot { ARMOR, AMULET }

@export var id: StringName
@export var display_name: String = "Equipamento"
@export var slot: Slot = Slot.ARMOR
## Fração de dano reduzida (0..0.9). Armaduras pesadas reduzem mais.
@export var damage_reduction: float = 0.0
@export var max_hp_bonus: float = 0.0
@export var stamina_regen_bonus: float = 0.0
@export_multiline var lore: String = ""
