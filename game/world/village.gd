extends Level
## Pedra-Alva. Além do comportamento de cena-base, dispara a cutscene do
## Ato 3 quando os quatro Santuários do Eco são restaurados.

const ACT3_CUTSCENE := PackedStringArray([
	"Quatro Santuários restaurados. Por toda Lirael, a névoa recua e as "
			+ "cores voltam, hesitantes, como quem acorda de um sono longo.",
	"No alto da vila, um selo de luz se abre: o caminho para o Coração "
			+ "Mudo, onde o Silêncio nasceu — e onde Selene ainda canta.",
	"Aria aperta a Ocarina. Do outro lado daquela porta está sua irmã. "
			+ "E a verdade sobre a Guardiã que preferiu o silêncio à dor.",
])


func _ready() -> void:
	super()
	if GameState.act2_complete() and not GameState.flags.get("cutscene_act3", false):
		GameState.flags["cutscene_act3"] = true
		_play_act3_cutscene.call_deferred()


func _play_act3_cutscene() -> void:
	GameState.pending_cutscene = ACT3_CUTSCENE
	GameState.cutscene_return = "res://world/village.tscn"
	GameState.next_spawn = "default"
	get_tree().change_scene_to_file("res://ui/cutscene.tscn")
