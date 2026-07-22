extends "res://addons/gut/test.gd"
## Testes das melodias da Ocarina (§3.1): as 8 carregam, têm efeito e sequências
## de notas distintas (o MelodyMatcher casa por sequência exata).

const PATHS := [
	"res://data/melodies/cancao_do_mundo.tres",
	"res://data/melodies/cancao_do_crepusculo.tres",
	"res://data/melodies/cancao_do_retorno.tres",
	"res://data/melodies/cancao_do_selo.tres",
	"res://data/melodies/cancao_da_cura.tres",
	"res://data/melodies/cancao_do_folego.tres",
	"res://data/melodies/cancao_da_coragem.tres",
	"res://data/melodies/cancao_do_refugio.tres",
]

func test_eight_melodies_load() -> void:
	for p in PATHS:
		var m: MelodyData = load(p)
		assert_not_null(m, "deve carregar %s" % p)
		assert_gt(m.notes.size(), 0)

func test_new_melodies_have_support_effects() -> void:
	assert_eq((load(PATHS[4]) as MelodyData).effect, MelodyData.Effect.HEAL)
	assert_eq((load(PATHS[5]) as MelodyData).effect, MelodyData.Effect.RESTORE_STAMINA)
	assert_eq((load(PATHS[6]) as MelodyData).effect, MelodyData.Effect.EMPOWER)
	assert_eq((load(PATHS[7]) as MelodyData).effect, MelodyData.Effect.REFUGE)

func test_note_sequences_are_unique() -> void:
	var seen: Array = []
	for p in PATHS:
		var key := String(load(p).notes)
		assert_false(seen.has(key), "sequência de notas duplicada em %s" % p)
		seen.append(key)
