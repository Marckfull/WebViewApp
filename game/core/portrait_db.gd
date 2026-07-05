class_name PortraitDB
## Mapeia o nome de quem fala ao seu retrato, exibido na caixa de diálogo.

const DIR := "res://assets/sprites/portraits/"
const DB := {
	"Aria": "aria",
	"Mestra Odara": "odara",
	"Corvo": "corvo",
	"Sela": "sela",
	"Selene": "selene",
	"Selene, a Guardiã Caída": "selene",
}


static func get_portrait(speaker: String) -> Texture2D:
	if not DB.has(speaker):
		return null
	return load(DIR + DB[speaker] + ".png")
