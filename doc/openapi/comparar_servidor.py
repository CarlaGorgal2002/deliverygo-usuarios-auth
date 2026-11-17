"""Compara operaciones, respuestas y seguridad del YAML con Springdoc en ejecución.

Uso: python doc/openapi/comparar_servidor.py http://localhost:8089
Requiere PyYAML (requirements.txt). No compara ejemplos ni sustituye las pruebas HTTP.
"""

import json
import sys
from pathlib import Path
from urllib.request import urlopen

import yaml


def operaciones(documento):
    metodos = {"get", "post", "put", "patch", "delete", "head", "options"}
    resultado = {}
    for ruta, contenido in documento["paths"].items():
        for metodo, operacion in contenido.items():
            if metodo not in metodos:
                continue
            seguridad = operacion.get("security", documento.get("security", []))
            # El orden de las alternativas OR y de los esquemas AND no cambia el contrato.
            seguridad = sorted(json.dumps(s, sort_keys=True) for s in seguridad)
            resultado[f"{metodo.upper()} {ruta}"] = {
                "respuestas": sorted(str(codigo) for codigo in operacion["responses"]),
                "seguridad": seguridad,
            }
    return resultado


if __name__ == "__main__":
    base = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8089"
    contrato = yaml.safe_load(Path(__file__).with_name("deliverygo-usuarios.yaml").read_text(encoding="utf-8"))
    with urlopen(base.rstrip("/") + "/v3/api-docs", timeout=10) as respuesta:
        servidor = json.load(respuesta)
    esperado, actual = operaciones(contrato), operaciones(servidor)
    diferencias = []
    for ruta in sorted(esperado.keys() | actual.keys()):
        if esperado.get(ruta) != actual.get(ruta):
            diferencias.append({"operacion": ruta, "yaml": esperado.get(ruta), "servidor": actual.get(ruta)})
    if diferencias:
        print(json.dumps(diferencias, ensure_ascii=False, indent=2))
        raise SystemExit(1)
    print(f"OK: {len(actual)} operaciones; respuestas y seguridad coinciden entre YAML y Springdoc.")
