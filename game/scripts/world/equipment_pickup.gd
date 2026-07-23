extends Interactable
## EquipmentPickup — coleta e equipa uma armadura/amuleto (§3.3). Ao pegar, guarda
## no arsenal (equipment_owned), equipa no slot correspondente e reaplica os bônus
## no Player. Simples e greybox: a peça encontrada já entra em uso.

@export var equipment: EquipmentData
@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	if equipment == null:
		return
	var owned: Array = SaveManager.state["equipment_owned"]
	if not owned.has(String(equipment.id)):
		owned.append(String(equipment.id))
	var slot_key := "armor" if equipment.slot == EquipmentData.Slot.ARMOR else "amulet"
	SaveManager.state["equipment"][slot_key] = String(equipment.id)
	SaveManager.save_game()  ## marco: autosave (§3.6)
	var p := get_tree().get_first_node_in_group("player")
	if p and p.has_method("apply_equipment"):
		p.apply_equipment()
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
