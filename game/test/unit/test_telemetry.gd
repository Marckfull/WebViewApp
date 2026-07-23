extends "res://addons/gut/test.gd"
## Testes da montagem/serialização de eventos de telemetria (§6.3).

func test_make_event_shape() -> void:
	var e := Telemetry.make_event("death", {"x": 10.0}, 3.456)
	assert_eq(e["type"], "death")
	assert_eq(e["data"]["x"], 10.0)
	assert_almost_eq(e["t"], 3.46, 0.001)

func test_to_line_is_valid_json() -> void:
	var line := Telemetry.to_line(Telemetry.make_event("rest", {"scene": "a"}, 1.0))
	var parsed: Variant = JSON.parse_string(line)
	assert_eq(typeof(parsed), TYPE_DICTIONARY)
	assert_eq(parsed["type"], "rest")

func test_roundtrip_preserves_data() -> void:
	var e := Telemetry.make_event("boss_defeated", {"boss": "guardia_do_eco"}, 42.0)
	var parsed: Dictionary = JSON.parse_string(Telemetry.to_line(e))
	assert_eq(parsed["data"]["boss"], "guardia_do_eco")
