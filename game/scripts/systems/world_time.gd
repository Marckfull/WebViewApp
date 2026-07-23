class_name WorldTime
extends RefCounted
## Tempo do mundo (§3.5). Lógica pura do ciclo dia/noite: o save guarda day_time
## (0.0 = dia, 0.5 = noite), alternado pela Canção do Crepúsculo. Centraliza o
## limiar para NPCs (lojas fecham à noite), inimigos noturnos e ambientação.

const NIGHT_THRESHOLD := 0.5

static func is_night(day_time: float) -> bool:
	return day_time >= NIGHT_THRESHOLD

static func is_day(day_time: float) -> bool:
	return not is_night(day_time)
