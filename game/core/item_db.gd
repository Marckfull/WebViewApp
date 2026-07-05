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
	"amuleto_eco": {
		"name": "Amuleto do Eco",
		"desc": "Um coração de vidro que pulsa devagar. +20 de vida máxima.",
		"icon": "res://assets/sprites/icons/amuleto_eco.png",
		"key": true,
	},
	"amuleto_vento": {
		"name": "Amuleto do Vento",
		"desc": "Leve como um suspiro. +20 de vigor máximo.",
		"icon": "res://assets/sprites/icons/amuleto_vento.png",
		"key": true,
	},
	"talisma_sela": {
		"name": "Talismã de Sela",
		"desc": "Era da mãe dela. Ataques e esquivas gastam 20% menos vigor.",
		"icon": "res://assets/sprites/icons/talisma_sela.png",
		"key": true,
	},
	"bomba_eco": {
		"name": "Bomba de Eco",
		"desc": "Som comprimido em pólvora. Lance com [B / BOMBA] — fere "
				+ "inimigos e derruba paredes rachadas.",
		"icon": "res://assets/sprites/icons/bomba_eco.png",
		"key": true,
	},
	"lente_verdade": {
		"name": "Lente da Verdade",
		"desc": "Um cristal polido pelas Guardiãs. Revela o que o Silêncio "
				+ "escondeu — segredos surgem só com ela na bolsa.",
		"icon": "res://assets/sprites/icons/lente_verdade.png",
		"key": true,
	},
	"memoria_lys": {
		"name": "Memória Perdida: Lys",
		"desc": "Um fragmento cristalizado. Dentro dele, a risada da sua "
				+ "irmã — a floresta a guardava.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_pedra_alva": {
		"name": "Memória Perdida: o Sino",
		"desc": "O sino da praça, tocando ao anoitecer. A Lente o trouxe "
				+ "de volta do Silêncio — escondido à vista de todos.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"ocarina_vidro": {
		"name": "Ocarina de Vidro",
		"desc": "O instrumento de uma Guardiã. Toque [M / OCARINA] para "
				+ "abrir a roda de melodias.",
		"icon": "res://assets/sprites/icons/ocarina_vidro.png",
		"key": true,
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
