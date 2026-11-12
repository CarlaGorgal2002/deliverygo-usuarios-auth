"""Genera YAML autónomos por área desde el contrato principal; no modifica la fuente."""
from copy import deepcopy
from pathlib import Path
import argparse
import yaml

ROOT = Path(__file__).resolve().parent
METHODS = {'get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'}
VIEWS = {'autenticacion': 'Autenticación', 'usuarios': 'Usuarios', 'direcciones': 'Direcciones'}


class Dumper(yaml.SafeDumper):
    def ignore_aliases(self, data):
        return True


def represent_string(dumper, value):
    return dumper.represent_scalar('tag:yaml.org,2002:str', value, style='|' if '\n' in value else None)


Dumper.add_representer(str, represent_string)


def references(value):
    if isinstance(value, dict):
        if '$ref' in value:
            yield value['$ref']
        for child in value.values():
            yield from references(child)
    elif isinstance(value, list):
        for child in value:
            yield from references(child)


def make_view(source, tag):
    view = deepcopy(source)
    view['info']['title'] += ' - ' + tag
    view['info']['description'] = (
        'Vista generada por área. Es parte del mismo servicio, no un microservicio independiente. '
        'El contrato completo y editable es deliverygo-usuarios.yaml.\n\n'
        + source['info']['description']
    )
    view['x-generado-desde'] = 'deliverygo-usuarios.yaml'
    view['tags'] = [t for t in view['tags'] if t['name'] == tag]
    view['paths'] = {}
    for path, item in source['paths'].items():
        operations = {m: deepcopy(op) for m, op in item.items() if m in METHODS and tag in op['tags']}
        if operations:
            # Preservar parámetros comunes del Path Item si se agregan en el futuro.
            operations.update({k: deepcopy(v) for k, v in item.items() if k not in METHODS})
            view['paths'][path] = operations
    components = {'securitySchemes': deepcopy(source['components']['securitySchemes'])}
    pending = set(references(view['paths']))
    copied = set()
    while pending:
        pointer = pending.pop()
        if pointer in copied:
            continue
        if not pointer.startswith('#/components/'):
            raise ValueError('Sólo se admiten referencias locales a componentes: ' + pointer)
        _, _, category, name = pointer.split('/')
        component = deepcopy(source['components'][category][name])
        components.setdefault(category, {})[name] = component
        copied.add(pointer)
        pending.update(set(references(component)) - copied)
    # Orden estable independiente del orden de los sets.
    view['components'] = {category: {name: values[name] for name in sorted(values)}
                          for category, values in sorted(components.items())}
    return view


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Verifica las vistas sin reescribirlas.')
    args = parser.parse_args()
    source = yaml.safe_load((ROOT / 'deliverygo-usuarios.yaml').read_text(encoding='utf-8'))
    for filename, tag in VIEWS.items():
        expected = make_view(source, tag)
        path = ROOT / 'vistas' / f'{filename}.yaml'
        if args.check:
            if not path.exists() or yaml.safe_load(path.read_text(encoding='utf-8')) != expected:
                raise SystemExit(f'Vista desactualizada: {path.name}. Ejecutar exportar_vistas.py.')
            print(f'OK vista sincronizada: {filename}.yaml')
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('# GENERADO: editar deliverygo-usuarios.yaml y ejecutar exportar_vistas.py.\n'
                            + yaml.dump(expected, Dumper=Dumper, allow_unicode=True, sort_keys=False, width=110), encoding='utf-8')
            print(f'Generado: {path.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
