class_name BestiaryDB
## Catálogo de criaturas (data-driven): nome e lore de cada inimigo,
## preenchido no Diário conforme a jogadora os derrota.

const DB := {
	"ecoado": {
		"name": "Ecoado",
		"desc": "O que resta de uma pessoa depois que o Silêncio apaga seu "
				+ "nome. Ataca por reflexo, não por vontade.",
	},
	"brutamontes": {
		"name": "Brutamontes",
		"desc": "Um Ecoado que em vida foi grande e forte. A guarda pesada "
				+ "só cede a golpes seguidos — ou a um parry certeiro.",
	},
	"espreitador": {
		"name": "Espreitador",
		"desc": "Rápido e frágil, corre pelas sombras da Floresta. Telegrafa "
				+ "pouco: leia o bote, não a distância.",
	},
	"forjado": {
		"name": "Forjado",
		"desc": "Vestido no próprio metal da Forja Afundada. Quase não "
				+ "vacila; quebre a postura antes de tentar o crítico.",
	},
	"sentinela": {
		"name": "Sentinela do Vento",
		"desc": "Guarda a Torre à distância, cuspindo rajadas que não se "
				+ "apara. Feche a distância ou use as colunas como abrigo.",
	},
	"carcaca": {
		"name": "Carcaça de Sal",
		"desc": "Guardiã menor, sepultada e ressecada na Necrópole. O sal "
				+ "preserva o corpo — e o rancor.",
	},
	"eco_guardia": {
		"name": "Eco da Guardiã",
		"desc": "O primeiro eco desperto na Cripta. Lembra, na segunda fase, "
				+ "de quem foi — e ataca em corrente tripla.",
	},
	"alfa": {
		"name": "Alfa do Bosque",
		"desc": "A fera que a Floresta escolheu para guardar a memória de "
				+ "Lys. Veloz e implacável em espaço aberto.",
	},
	"coracao_forja": {
		"name": "Coração da Forja",
		"desc": "Um golem de ferro e brasa afogado na Forja. Lento, mas cada "
				+ "golpe pesa como uma bigorna.",
	},
	"guardiao_ventos": {
		"name": "Guardião dos Ventos",
		"desc": "Espírito do ar no ápice da Torre. Guardava a Lente da "
				+ "Verdade — e o vento obedecia a ele.",
	},
	"rainha_sal": {
		"name": "Rainha de Sal",
		"desc": "A Guardiã que acordou sob a Necrópole. Cristalina e cruel, "
				+ "protegia as Botas de Corrente.",
	},
	"selene": {
		"name": "Selene, a Guardiã Caída",
		"desc": "A última Guardiã. Não criou o Silêncio por maldade, mas "
				+ "para não ouvir mais a canção que a fazia lembrar.",
	},
}


static func get_entry(id: String) -> Dictionary:
	return DB.get(id, {})
