extends CanvasLayer
## Controles de toque: visíveis apenas em dispositivos com touchscreen.
## No desktop, teclado (WASD + J/K/E) e gamepad continuam funcionando.


func _ready() -> void:
	visible = DisplayServer.is_touchscreen_available()
