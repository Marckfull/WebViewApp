class_name ShopData
extends Resource
## Dados da loja de um mercador (§3.5). Data-driven: cada NPC-loja aponta para um
## ShopData. As ofertas são arrays paralelos (mesmo padrão de MelodyData) para
## manter os .tres simples e legíveis.

@export var shop_id: StringName
@export var title: String = "Mercador"
## Fala de abertura (lore do mercador), mostrada no topo da loja.
@export_multiline var greeting: String = ""
## Ofertas: cada índice é um item à venda.
@export var item_ids: Array[StringName] = []
@export var labels: Array[String] = []
@export var prices: Array[int] = []
## Bolsa de destino de cada item: "consumables" (poções) ou "resources" (craft).
@export var bags: Array[String] = []

## Nº de ofertas válidas (o menor comprimento entre os arrays paralelos).
func offer_count() -> int:
	var n := item_ids.size()
	n = mini(n, labels.size())
	n = mini(n, prices.size())
	n = mini(n, bags.size())
	return n
