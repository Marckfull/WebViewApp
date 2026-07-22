class_name LocaleTable
extends RefCounted
## Lógica pura de tradução (§Alpha→Beta): resolve uma chave num conjunto de
## tabelas por idioma, com fallback para o idioma-fonte (pt_BR) e, por fim, a
## própria chave. Sem IO/estado — o autoload Locale carrega as tabelas.

static func get_text(tables: Dictionary, locale: String, key: String, fallback_locale: String = "pt_BR") -> String:
	var t: Variant = tables.get(locale, {})
	if t is Dictionary and t.has(key):
		return String(t[key])
	var f: Variant = tables.get(fallback_locale, {})
	if f is Dictionary and f.has(key):
		return String(f[key])
	return key
