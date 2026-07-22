extends Node
## Locale — localização em runtime (autoload, §Alpha→Beta). Carrega as tabelas de
## idioma (JSON) e resolve chaves via LocaleTable (puro). Idioma persistido nas
## settings do save. Interface enxuta t(key); pode migrar para gettext depois.

const LOCALES := ["pt_BR", "en"]
const PATHS := {
	"pt_BR": "res://data/locale/pt_BR.json",
	"en": "res://data/locale/en.json",
}

var _tables: Dictionary = {}
var current: String = "pt_BR"

func _ready() -> void:
	for loc in LOCALES:
		_tables[loc] = _load_table(PATHS[loc])
	current = String(SaveManager.state.get("settings", {}).get("locale", "pt_BR"))
	if not LOCALES.has(current):
		current = "pt_BR"

func _load_table(path: String) -> Dictionary:
	if not FileAccess.file_exists(path):
		return {}
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return {}
	var parsed: Variant = JSON.parse_string(f.get_as_text())
	return parsed if parsed is Dictionary else {}

func t(key: String) -> String:
	return LocaleTable.get_text(_tables, current, key)

func set_locale(loc: String) -> void:
	if not LOCALES.has(loc):
		return
	current = loc
	SaveManager.state["settings"]["locale"] = loc
	SaveManager.save_game()

## Alterna PT-BR <-> EN (para o toggle de opções).
func toggle() -> void:
	set_locale("en" if current == "pt_BR" else "pt_BR")

func short_name() -> String:
	return "EN" if current == "en" else "PT"
