extends "res://addons/gut/test.gd"
## Testes da localização (§Alpha→Beta): lookup puro com fallback, e integridade
## das tabelas PT-BR/EN (mesmas chaves).

var _tables := {
	"pt_BR": {"HELLO": "Olá", "ONLY_PT": "Só PT"},
	"en": {"HELLO": "Hello"},
}

func test_returns_locale_text() -> void:
	assert_eq(LocaleTable.get_text(_tables, "en", "HELLO"), "Hello")
	assert_eq(LocaleTable.get_text(_tables, "pt_BR", "HELLO"), "Olá")

func test_falls_back_to_source_locale() -> void:
	# EN não tem ONLY_PT -> cai para pt_BR.
	assert_eq(LocaleTable.get_text(_tables, "en", "ONLY_PT"), "Só PT")

func test_missing_key_returns_key() -> void:
	assert_eq(LocaleTable.get_text(_tables, "en", "NAO_EXISTE"), "NAO_EXISTE")

func test_unknown_locale_falls_back() -> void:
	assert_eq(LocaleTable.get_text(_tables, "fr", "HELLO"), "Olá")

func _read_json(path: String) -> Dictionary:
	var f := FileAccess.open(path, FileAccess.READ)
	assert_not_null(f, "deve abrir %s" % path)
	var d: Variant = JSON.parse_string(f.get_as_text())
	assert_true(d is Dictionary, "%s deve ser JSON objeto" % path)
	return d

func test_json_tables_have_matching_keys() -> void:
	var pt := _read_json("res://data/locale/pt_BR.json")
	var en := _read_json("res://data/locale/en.json")
	for k in pt.keys():
		assert_true(en.has(k), "EN deve ter a chave %s" % k)
	assert_eq(pt.keys().size(), en.keys().size(), "PT e EN: mesmo nº de chaves")
