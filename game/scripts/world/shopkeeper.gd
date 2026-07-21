extends Interactable
## Shopkeeper — mercador que abre uma loja ao interagir (§3.5). Data-driven: as
## ofertas e a lore vêm de um ShopData. Usado por Corvo, o mercador errante.

@export var shop: ShopData

func interact(_player: Node) -> void:
	if shop:
		ShopMenu.open(shop)
