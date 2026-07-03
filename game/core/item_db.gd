class_name ItemDB
## Catálogo de itens (data-driven): nome, descrição, ícone e se é
## item-chave. Novos itens entram só aqui.

const DB := {
	"minerio_eco": {
		"name": "Minério de Eco",
		"desc": "Metal que ainda vibra com a Canção. Odara saberia forjá-lo.",
		"icon": "res://assets/sprites/icons/minerio_eco.png",
	},
	"erva_lunar": {
		"name": "Erva-lunar",
		"desc": "Brilha de leve no escuro. Base de poções restauradoras.",
		"icon": "res://assets/sprites/icons/erva_lunar.png",
	},
	"gancho_corda": {
		"name": "Gancho-corda",
		"desc": "Ferramenta das Guardiãs. Use-a [H / ITEM] perto de um poste "
				+ "com argola para se lançar até ele.",
		"icon": "res://assets/sprites/icons/gancho_corda.png",
		"key": true,
	},
}


static func get_item(id: String) -> Dictionary:
	return DB.get(id, {})
