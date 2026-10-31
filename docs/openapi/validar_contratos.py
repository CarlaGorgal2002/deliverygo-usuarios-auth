"""Valida OpenAPI, referencias, ejemplos y reglas contractuales; no ejecuta el backend."""
from copy import deepcopy
from pathlib import Path
import re
import yaml
from openapi_spec_validator import validate
from openapi_schema_validator import OAS30Validator, OAS30WriteValidator, OAS30ReadValidator
from exportar_vistas import METHODS, VIEWS, make_view

ROOT = Path(__file__).resolve().parent


class UniqueLoader(yaml.SafeLoader):
    pass


def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise ValueError(f'Clave YAML duplicada: {key!r}, línea {key_node.start_mark.line + 1}')
        result[key] = loader.construct_object(value_node, deep=deep)
    return result


UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)


def load(path):
    return yaml.load(path.read_text(encoding='utf-8'), Loader=UniqueLoader)


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def resolve(value, doc):
    if isinstance(value, dict):
        if '$ref' in value:
            pointer = value['$ref']
            require(pointer.startswith('#/'), 'Referencia externa no autónoma: ' + pointer)
            node = doc
            for part in pointer[2:].split('/'):
                node = node[part.replace('~1', '/').replace('~0', '~')]
            return resolve(node, doc)
        return {k: resolve(v, doc) for k, v in value.items()}
    if isinstance(value, list):
        return [resolve(v, doc) for v in value]
    return value


def validator(schema, direction=None):
    kind = OAS30WriteValidator if direction == 'request' else OAS30ReadValidator if direction == 'response' else OAS30Validator
    return kind(schema, format_checker=OAS30Validator.FORMAT_CHECKER)


def check_document(path):
    doc = load(path)
    validate(doc)
    expanded = resolve(doc, doc)
    example_count = 0

    def walk(node, context):
        nonlocal example_count
        if isinstance(node, dict):
            # Media Type, Header y Parameter tienen schema + ejemplos.
            if 'schema' in node:
                direction = 'request' if '/requestBody/' in context else 'response' if '/responses/' in context else None
                v = validator(node['schema'], direction)
                if 'example' in node:
                    v.validate(node['example'])
                    example_count += 1
                for name, ex in node.get('examples', {}).items():
                    require('value' in ex, f'{context}: ejemplo externo no admitido')
                    v.validate(ex['value'])
                    example_count += 1
            # Ejemplos de propiedades dentro de Schema Objects.
            elif 'type' in node and 'example' in node:
                validator(node).validate(node['example'])
                example_count += 1
            for key, value in node.items():
                walk(value, context + '/' + key)
        elif isinstance(node, list):
            for i, value in enumerate(node):
                walk(value, context + '/' + str(i))

    walk(expanded, path.name)
    ids = set()
    for route, item in expanded['paths'].items():
        for method, op in item.items():
            if method not in METHODS:
                continue
            require(op['operationId'] not in ids, 'operationId repetido')
            ids.add(op['operationId'])
            names = {p['name'] for p in item.get('parameters', []) + op.get('parameters', []) if p['in'] == 'path'}
            require(names == set(re.findall(r'{([^}]+)}', route)), 'Parámetros de ruta incorrectos: ' + route)
            security = op.get('security', expanded.get('security'))
            is_public = route in ['/api/v1/auth/register', '/api/v1/auth/login']
            require(security == ([] if is_public else [{'bearerAuth': []}]), 'Seguridad incorrecta: ' + route)
            if method in ['get', 'delete'] or route.endswith('/default'):
                require('requestBody' not in op, 'Cuerpo inesperado: ' + route)
            for status, response in op['responses'].items():
                require('X-Correlation-Id' in response['headers'], 'Falta correlación')
                if status == '204':
                    require('content' not in response, '204 debe carecer de cuerpo')
                if status == '401':
                    require('WWW-Authenticate' in response['headers'], 'Falta desafío en 401')
                if int(status) >= 400:
                    media = response['content']['application/json']
                    for example in media['examples'].values():
                        error = example['value']
                        require(error['status'] == int(status), 'status no coincide con HTTP')
                        concrete = route.replace('{id}', '1').replace('{addressId}', '10')
                        require(error['path'] == concrete, 'Ejemplo de error usa otra ruta')
                        require(('details' in error) == (error['code'] == 'VALIDATION_ERROR'), 'details inconsistente')
                else:
                    # La contraseña o su hash nunca deben aparecer en una salida.
                    serialized = str(response.get('content', {}))
                    require("'password':" not in serialized and "'passwordHash':" not in serialized, 'Salida sensible')
    print(f'OK {path.relative_to(ROOT)}: {len(ids)} operaciones; {example_count} comprobaciones de ejemplos (incluye referencias expandidas).')
    return doc, expanded


def check_business_contract(doc):
    schemas = doc['components']['schemas']
    scenarios = 0

    def expect(schema, value, valid):
        nonlocal scenarios
        actual = validator(schemas[schema], 'request').is_valid(value)
        require(actual == valid, f'Escenario inesperado en {schema}: {value}')
        scenarios += 1

    profile = {'nombre': 'Ana', 'apellido': 'Pérez', 'email': 'ana@example.com'}
    register = {**profile, 'password': 'ClaveDemo2026!'}
    address = {'calle': 'Lima', 'numero': 'S/N', 'ciudad': 'CABA', 'provincia': 'CABA', 'codigoPostal': 'C1073AAO'}
    expect('RegistrarUsuarioRequest', register, True)
    for field in ['id', 'rol', 'estado', 'passwordHash']:
        expect('RegistrarUsuarioRequest', {**register, field: 'ADMIN'}, False)
    for field in ['nombre', 'apellido', 'email', 'password']:
        invalid = deepcopy(register)
        del invalid[field]
        expect('RegistrarUsuarioRequest', invalid, False)
    for value in ['', '   ', '\t', None]:
        expect('RegistrarUsuarioRequest', {**register, 'nombre': value}, False)
    expect('RegistrarUsuarioRequest', {**register, 'password': '1234567'}, False)
    expect('RegistrarUsuarioRequest', {**register, 'password': '        '}, False)
    expect('RegistrarUsuarioRequest', {**register, 'email': 'sin-arroba'}, False)
    expect('LoginRequest', {'username': 'ana@example.com', 'password': 'ClaveDemo2026!'}, False)
    expect('LoginRequest', {'email': 'ana@example.com', 'password': 'x'}, True)
    expect('LoginRequest', {'email': 'ana@example.com', 'password': '   '}, False)
    expect('ModificarPerfilRequest', profile, True)
    expect('ModificarPerfilRequest', {**profile, 'telefono': None}, True)
    expect('ModificarPerfilRequest', {'nombre': 'Ana'}, False)
    for field in ['rol', 'estado', 'password']:
        expect('ModificarPerfilRequest', {**profile, field: 'ADMIN'}, False)
    expect('CrearDireccionRequest', address, True)
    expect('CrearDireccionRequest', {**address, 'predeterminada': True}, True)
    expect('CrearDireccionRequest', {**address, 'predeterminada': 'true'}, False)
    expect('CrearDireccionRequest', {**address, 'predeterminada': None}, False)
    expect('CrearDireccionRequest', {**address, 'id': 10}, False)
    expect('ModificarDireccionRequest', address, True)
    expect('ModificarDireccionRequest', {**address, 'piso': None, 'departamento': None}, True)
    for field in ['predeterminada', 'activa', 'usuarioId', 'referencia']:
        expect('ModificarDireccionRequest', {**address, field: True}, False)
    expect('ModificarDireccionRequest', {'calle': 'Lima'}, False)
    expect('EstadoUsuario', 'BLOQUEADO', False)
    expect('Rol', 'MODULO_PEDIDOS', False)
    # Reglas de frontera verificables sin ejecutar el backend.
    for parameter in ['UsuarioId', 'DireccionId']:
        v = validator(doc['components']['parameters'][parameter]['schema'])
        for value, valid in [(1, True), (0, False), (-1, False), (1.5, False), ('1', False), (9223372036854775808, False)]:
            require(v.is_valid(value) == valid, f'ID inválido aceptado: {parameter} {value}')
            scenarios += 1
    require(schemas['CrearDireccionRequest']['properties']['predeterminada']['default'] is False, 'Predeterminada por omisión')
    expected = {
        ('post','/api/v1/auth/register'), ('post','/api/v1/auth/login'),
        ('get','/api/v1/users/{id}'), ('put','/api/v1/users/{id}'),
        ('get','/api/v1/users/{id}/addresses'), ('post','/api/v1/users/{id}/addresses'),
        ('put','/api/v1/users/{id}/addresses/{addressId}'), ('delete','/api/v1/users/{id}/addresses/{addressId}'),
        ('patch','/api/v1/users/{id}/addresses/{addressId}/default')}
    actual = {(method, route) for route, item in doc['paths'].items() for method in item if method in METHODS}
    require(actual == expected, 'Cobertura CORE incorrecta')
    for route, item in doc['paths'].items():
        for method, op in item.items():
            if method not in METHODS:
                continue
            for status, resp in op['responses'].items():
                for media in resp.get('content', {}).values():
                    for ex in media.get('examples', {}).values():
                        value = ex['value']
                        if isinstance(value, dict) and value.get('code') == 'USER_INACTIVE':
                            require(status == '403', 'Cuenta inactiva debe ser 403')
    print(f'OK {scenarios} escenarios positivos/negativos y cobertura exacta de las 9 operaciones CORE.')


def main():
    source, expanded = check_document(ROOT / 'deliverygo-usuarios.yaml')
    check_business_contract(expanded)
    for filename, tag in VIEWS.items():
        path = ROOT / 'vistas' / f'{filename}.yaml'
        view, _ = check_document(path)
        require(view == make_view(source, tag), 'Vista desactualizada: ' + filename)
    print('RESULTADO: contratos válidos y vistas sincronizadas. Valida el documento, no el servidor: las pruebas del backend se ejecutan con mvn test.')


if __name__ == '__main__':
    main()
