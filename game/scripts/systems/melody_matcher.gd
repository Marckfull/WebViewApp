class_name MelodyMatcher
extends RefCounted
## Lógica pura de reconhecimento de melodias da Ocarina (§3.1), extraída do
## OcarinaManager para ser testável isoladamente (§6.3). Sem estado, sem UI.

## True se `seq` é prefixo (início) de `full`.
static func is_prefix(seq: Array, full: Array) -> bool:
	if seq.size() > full.size():
		return false
	for i in seq.size():
		if seq[i] != full[i]:
			return false
	return true

## Retorna a MelodyData cujas notas são exatamente `entered`, ou null.
static func exact_match(entered: Array, melodies: Array) -> MelodyData:
	for m in melodies:
		if entered == m.notes:
			return m
	return null

## True se `entered` ainda pode virar alguma melodia (é prefixo de pelo menos uma).
static func any_prefix(entered: Array, melodies: Array) -> bool:
	for m in melodies:
		if is_prefix(entered, m.notes):
			return true
	return false
