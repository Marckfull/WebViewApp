extends CanvasModulate
## DayNight — tinge a cena inteira conforme o horário (§3.5, §4: "a cor conta a
## história"). Dia = tom natural; noite = azul-frio mais escuro. A melodia da
## Ocarina (efeito DAY_NIGHT) alterna e este nó anima a transição.

const DAY_COLOR := Color(1, 1, 1)
const NIGHT_COLOR := Color(0.5, 0.56, 0.82)

func _ready() -> void:
	GameEvents.day_time_changed.connect(_on_day_time_changed)
	_apply(SaveManager.state["world"].get("day_time", 0.0), false)

func _on_day_time_changed(value: float) -> void:
	_apply(value, true)

func _apply(day_time: float, animate: bool) -> void:
	var target := NIGHT_COLOR if day_time >= 0.5 else DAY_COLOR
	if animate:
		create_tween().tween_property(self, "color", target, 0.8)
	else:
		color = target
