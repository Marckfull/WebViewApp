class_name Telemetry
extends RefCounted
## Lógica pura de telemetria de playtest (§6.3) — monta registros de evento.
## Sem I/O; o TelemetryLogger (autoload) cuida de habilitar/gravar. Opt-in.

## Monta um registro serializável de um evento (tipo + dados + tempo).
static func make_event(type: String, data: Dictionary, playtime: float) -> Dictionary:
	return {
		"t": snappedf(playtime, 0.01),
		"type": type,
		"data": data,
	}

## Serializa um registro em uma linha JSON (formato JSONL).
static func to_line(event: Dictionary) -> String:
	return JSON.stringify(event)
