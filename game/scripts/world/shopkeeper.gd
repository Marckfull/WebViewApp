extends Interactable
## Shopkeeper — mercador que abre uma loja ao interagir (§3.5). Data-driven: as
## ofertas e a lore vêm de um ShopData. Usado por Corvo, o mercador errante.

@export var shop: ShopData
## Fala quando a loja está fechada (à noite, §3.5). Se vazia, a loja abre sempre.
@export var closed_dialogue: DialogueData

func interact(_player: Node) -> void:
	if closed_dialogue and WorldTime.is_night(SaveManager.state["world"].get("day_time", 0.0)):
		DialogueManager.start(closed_dialogue)
		return
	if shop:
		ShopMenu.open(shop)
