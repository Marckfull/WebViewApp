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
	"botas_corrente": {
		"name": "Botas de Corrente",
		"desc": "Ferradas com sal-ímã. Você atravessa as correntes que "
				+ "varreriam qualquer outro — e o vento perde a força.",
		"icon": "res://assets/sprites/icons/botas_corrente.png",
		"key": true,
	},
	"memoria_necropole": {
		"name": "Memória Perdida: a Guardiã",
		"desc": "O rosto de Selene antes da dor. Ela ria, um dia. A "
				+ "corrente de sal guardava isto.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_cripta": {
		"name": "Memória Perdida: o Juramento",
		"desc": "Cinco Guardiãs de mãos dadas na Cripta, jurando guardar a "
				+ "Canção. Uma delas chora — ninguém percebe.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_forja": {
		"name": "Memória Perdida: a Bigorna",
		"desc": "Odara jovem, martelando a primeira lâmina que cantou. O "
				+ "fogo da Forja ainda ardia alto.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_torre": {
		"name": "Memória Perdida: o Voo",
		"desc": "Uma criança soltando pipa no topo da Torre, o vento inteiro "
				+ "só dela. Antes de o ar virar arma.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_bosque": {
		"name": "Memória Perdida: a Trilha",
		"desc": "Aria e Lys marcando árvores para não se perder. O primeiro "
				+ "mapa que Aria desenhou foi por amor.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_treino": {
		"name": "Memória Perdida: a Estocada",
		"desc": "Aria aprendendo a segurar uma lâmina, desajeitada, rindo. "
				+ "Ninguém imaginava para o que serviria.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_corvo": {
		"name": "Memória Perdida: a Estrada",
		"desc": "Corvo em outra vida, com um nome e uma casa. Ele vendeu "
				+ "esta lembrança sem dizer que era a própria.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_sela": {
		"name": "Memória Perdida: o Canteiro",
		"desc": "A mãe de Sela cantando entre as ervas-lunares ao anoitecer. "
				+ "A canção que virou o Talismã.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_odara": {
		"name": "Memória Perdida: a Mestra",
		"desc": "Odara entregando a Aria criança uma bússola torta. \"Toda "
				+ "cartógrafa começa se perdendo\", ela disse.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
		"key": true,
	},
	"memoria_coracao": {
		"name": "Memória Perdida: o Silêncio",
		"desc": "O instante em que Selene escolheu emudecer o mundo. Não há "
				+ "ódio nela — só um cansaço fundo de sentir.",
		"icon": "res://assets/sprites/icons/memoria_lys.png",
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
