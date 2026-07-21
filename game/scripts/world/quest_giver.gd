extends Interactable
## QuestGiver — NPC que oferece, acompanha e conclui uma side quest (§3.5).

@export var quest: QuestData
@export var offer_dialogue: DialogueData
@export var progress_dialogue: DialogueData
@export var complete_dialogue: DialogueData

func interact(_player: Node) -> void:
	if quest == null:
		return
	var quests: Dictionary = SaveManager.state["quests"]
	match Quest.status(quests, quest.id):
		Quest.UNSTARTED:
			QuestManager.start(quest)
			_say(offer_dialogue)
		Quest.ACTIVE:
			if Quest.is_ready_to_complete(quest, quests):
				QuestManager.turn_in(quest)
				_say(complete_dialogue)
			else:
				_say(progress_dialogue)
		_:
			_say(complete_dialogue)

func _say(d: DialogueData) -> void:
	if d:
		DialogueManager.start(d)
