"""Extrae evidencia de Postman CLI sin copiar tokens, claves ni cuerpos HTTP.

Uso: python doc/postman/resumir_resultado.py target/resultado.json target/resumen.json
Compatible con el reporte JSON de Postman CLI 1.73.0.
"""

import json
import sys
from pathlib import Path


def resumir(origen: Path, destino: Path) -> None:
    corrida = json.loads(origen.read_text(encoding="utf-8"))["run"]
    resumen = {
        "coleccion": corrida["meta"]["collectionName"],
        "inicio": corrida["meta"]["started"],
        "fin": corrida["meta"]["completed"],
        "resumen": corrida["summary"],
        "errorDeEjecucion": corrida.get("runError") is not None,
        "solicitudes": [
            {
                "nombre": paso["requestExecuted"]["name"],
                "metodo": paso["requestExecuted"]["method"],
                "status": (paso.get("response") or {}).get("code"),
                "erroresDeEjecucion": len(paso.get("errors", [])),
            }
            for paso in corrida["executions"]
        ],
    }
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(json.dumps(resumen, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Uso: python resumir_resultado.py reporte.json resumen.json")
    resumir(Path(sys.argv[1]), Path(sys.argv[2]))
