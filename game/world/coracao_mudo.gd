extends Level
## O Coração Mudo: o confronto final com Selene. Ela fala antes de lutar;
## ao ser derrotada, a história chega ao dilema (tela de finais).

static var SELENE_LINES := PackedStringArray([
	"Você atravessou o Silêncio inteiro para chegar até aqui, cartógrafa.",
	"Eu não o criei por maldade. Criei para não ouvir mais a canção que "
			+ "me lembra tudo o que perdi.",
	"Sua irmã está segura — o Silêncio guarda, não mata. Mas eu não posso "
			+ "deixar você me acordar. Dói demais.",
	"Então lute. Prove que a dor vale ser ouvida.",
])

var _started := false

@onready var boss: BossEcoGuardia = $Boss
@onready var trigger: Area2D = $BossTrigger


func _ready() -> void:
	super()
	GameEvents.boss_ended.connect(_on_boss_ended)
	GameEvents.shrine_rested.connect(_on_rest)
	trigger.body_entered.connect(_on_trigger_entered)
	# Pós-jogo: Selene já foi enfrentada; a arena fica em paz.
	if GameState.flags.get("selene_derrotada", false):
		boss.queue_free()
		trigger.queue_free()
		_started = true


func _on_trigger_entered(body: Node2D) -> void:
	if _started or not (body is Player and body.is_alive()):
		return
	_started = true
	# O diálogo pausa a árvore; o boss desperta e ataca ao fechar a fala.
	GameEvents.dialog_requested.emit("Selene", SELENE_LINES)
	boss.activate()


func _on_boss_ended(victory: bool) -> void:
	if not victory:
		return
	GameState.flags["selene_derrotada"] = true
	await get_tree().create_timer(1.8).timeout
	get_tree().change_scene_to_file("res://ui/ending.tscn")


func _on_rest() -> void:
	if is_instance_valid(boss):
		boss.reset()
		_started = false
