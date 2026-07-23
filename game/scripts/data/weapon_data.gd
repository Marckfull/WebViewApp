class_name WeaponData
extends Resource
## WeaponData — arma data-driven (§3.3, §6.2).
##
## Cada arma muda o moveset. Designers criam .tres na pasta data/weapons/ sem
## tocar código. 6 armas planejadas: adaga dupla, espada+escudo, lança, martelo,
## arco, chicote-corda.

enum WeaponClass { ADAGA_DUPLA, ESPADA_ESCUDO, LANCA, MARTELO, ARCO, CHICOTE }

@export var id: StringName
@export var display_name: String = "Arma"
@export var weapon_class: WeaponClass = WeaponClass.ESPADA_ESCUDO
@export var base_damage: float = 12.0
@export var poise_damage: float = 15.0
@export var stamina_cost: float = 20.0
@export var attack_speed: float = 1.0          ## multiplicador de velocidade da animação
@export var combo_length: int = 3              ## 3 ataques por arma (§4)
@export var reach: float = 18.0                ## distância do golpe (lança longe, martelo perto)
@export_multiline var lore: String = ""        ## descrição rica em lore (§3.5)
