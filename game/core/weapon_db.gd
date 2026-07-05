class_name WeaponDB
## Armas empunháveis, cada uma com seu moveset (multiplicadores sobre o
## ataque base da Aria). "combo" é o índice máximo de encadeamento.

const DB := {
	"espada": {
		"name": "Espada da Guardiã",
		"desc": "Equilibrada: o padrão em que Aria treinou.",
		"dmg": 1.0, "stamina": 1.0, "speed": 1.0, "reach": 1.0,
		"knockback": 1.0, "lunge": 1.0, "combo": 2,
	},
	"adaga": {
		"name": "Adaga Dupla",
		"desc": "Rápida e barata, mas fraca. Encadeia até quatro golpes.",
		"dmg": 0.62, "stamina": 0.6, "speed": 0.72, "reach": 0.85,
		"knockback": 0.7, "lunge": 1.25, "combo": 3,
	},
	"martelo": {
		"name": "Martelo da Forja",
		"desc": "Lento e pesado: dano alto, recuo enorme, quebra guardas.",
		"dmg": 1.9, "stamina": 1.7, "speed": 1.4, "reach": 1.2,
		"knockback": 1.9, "lunge": 0.8, "combo": 1,
	},
}


static func get_weapon(id: String) -> Dictionary:
	return DB.get(id, DB["espada"])
