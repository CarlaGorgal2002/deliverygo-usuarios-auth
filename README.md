# deliverygo-usuarios-auth
Módulo de Usuarios y Autenticación - TPO DeliveryGo - Equipo 2


## Configuración local

El archivo `src/main/resources/application.properties` **no se versiona** (está en el `.gitignore`) para que cada uno pueda tener su propia configuración y credenciales sin subirlas al repo.

En su lugar se versiona el template `src/main/resources/application.properties.example`.

### Primer uso

1. Copiar el template:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Completar los valores en `application.properties`. Para la base H2 en memoria que se usa actualmente:
   ```properties
   spring.datasource.username=sa
   spring.datasource.password=
   ```

### Si agregás una propiedad nueva

Agregala también en `application.properties.example` (sin datos sensibles) para que el resto del equipo la tenga.
