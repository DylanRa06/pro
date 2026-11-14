# Patitas al Rescate

API de Patitas al Rescate: base individual E1-E4 y ampliación de seguridad grupal.
La ampliación implementa los temas anunciados en el PDF individual; no se recibió un enunciado grupal detallado.
Java 21, Spring Boot 3.5.7, Maven y PostgreSQL. Paquete base: com.krakedev.pro.
Se conserva el nombre del proyecto pro del repositorio; el paquete sugerido del PDF es opcional.

## Ejecutar en Windows y Eclipse

1. Crear patitas_db en pgAdmin: clic derecho en Databases > Create > Database.
2. En la raíz del proyecto, copiar application-local.properties.example como
   application-local.properties y escribir allí las credenciales locales de PostgreSQL.
   Ese archivo está excluido de Git. No cambiar el archivo de ejemplo para poner claves reales.
3. También se pueden definir DB_URL, DB_USERNAME y DB_PASSWORD como variables de entorno.
   La URL predeterminada es jdbc:postgresql://localhost:5432/patitas_db.
4. Importar con File > Import > Maven > Existing Maven Projects.
   Si ya está importado, F5 y Maven > Update Project.
5. Ejecutar com.krakedev.pro.ProApplication como Java Application.
   Esperar Started ProApplication y Tomcat en el puerto 8080.
6. Como alternativa: mvnw.cmd spring-boot:run desde CMD en la raíz.
   En Git Bash: ./mvnw spring-boot:run.

## Pruebas

Importar postman/Patitas_Grupal.postman_collection.json. La variable baseUrl vale
http://localhost:8080. Las carpetas 1, 2 y 3 contienen las pruebas en ese orden.
La colección Individual se conserva como referencia histórica: sus llamadas públicas a
mascotas corresponden a la entrega anterior y ahora necesitan token y permisos.

| Ruta | Acceso | Resultado correcto |
| --- | --- | --- |
| POST /api/auth/registrar | Público | 201, siempre rol USER |
| POST /api/auth/login | Público | 200 con JWT o 401 |
| GET /api/auth/perfil | USER o ADMIN con JWT válido | 200 |
| GET /api/mascotas | USER o ADMIN con JWT válido | 200 |
| POST /api/mascotas | Solo ADMIN con JWT válido | 201 |
| POST /api/auth/logout | USER o ADMIN con JWT válido | 200 y revoca ese token |

Sin credencial válida: 401. Con credencial válida sin permiso: 403.
La asignación de permisos anterior es una decisión de implementación basada en los roles
del refugio; el PDF individual no contiene una matriz de permisos grupal detallada.

## Preparar ADMIN local

1. Registrar admin_grupal desde Postman; el registro público lo guardará como USER.
2. En pgAdmin, conectado a patitas_db, ejecutar como administrador de la base:

    UPDATE public.usuarios SET rol = 'ADMIN' WHERE username = 'admin_grupal';
    SELECT id, username, rol FROM public.usuarios WHERE username = 'admin_grupal';

3. Hacer login de nuevo como admin_grupal para emitir su JWT con rol ADMIN.
   Un token anterior mantiene el rol con el que fue emitido hasta vencer o cerrarse.
   No se permite elegir ADMIN en el JSON del registro público.

## JWT y logout

JwtAuthenticationFilter valida firma HS256, emisor patitas-rescate, expiración,
subject, rol y jti. También consulta tokens_revocados antes de crear la autenticación
en SecurityContextHolder. @PreAuthorize restringe las acciones de mascotas.
SecurityConfig configura rutas públicas, respuestas 401/403 y sesiones STATELESS.
No se usan sesiones de navegador ni cookies para autenticar estas peticiones.

POST /api/auth/logout guarda el jti y vencimiento en tokens_revocados y rechaza
las siguientes peticiones con ese token, incluso si no ha expirado. El logout solo
revoca el token presentado; otros tokens del mismo usuario siguen siendo válidos.
Las filas de revocación se conservan en PostgreSQL; esta versión no elimina las vencidas.

JWT firmado no significa cifrado: no se colocan contraseñas en el payload.
Sin JWT_SECRET se genera una clave aleatoria por arranque; todos los tokens anteriores
dejarán de ser válidos al reiniciar. Para conservarlos entre arranques, definir
JWT_SECRET por entorno con al menos 32 bytes y mantener la misma clave local.
La blacklist persiste en PostgreSQL aunque se reinicie el servidor.

En pgAdmin pueden revisarse los datos:

    SELECT * FROM public.mascotas ORDER BY id;
    SELECT id, username, rol FROM public.usuarios ORDER BY id;
    SELECT jti, expiracion FROM public.tokens_revocados;

El cliente no puede modificar mascotas existentes enviando un id al crear.

## Comprobación automática

    mvnw.cmd clean verify

En Git Bash: bash mvnw clean verify. GitHub Actions también ejecuta la verificación.

Los tests usan H2 en memoria (modo PostgreSQL) sin tocar patitas_db.
Las capturas de Postman, consola y PostgreSQL se toman en el equipo donde se ejecuta la práctica.

## Preguntas técnicas

### E1
1. @Entity indica que JPA debe mapear la clase a una tabla. @Id identifica la clave primaria
   que distingue cada fila; @GeneratedValue permite generarla.
2. Una clase define atributos y comportamiento; un objeto es una instancia en memoria.
   Una entidad persistente es un objeto con identidad que JPA puede guardar y recuperar.

### E2
3. El Service aplica las reglas del negocio y coordina operaciones; el Repository se ocupa
   de consultar y persistir. Separarlos evita mezclar validaciones con el acceso a los datos.
4. @PostMapping atiende POST para registrar datos; @GetMapping atiende GET para consultarlos.
   En esta API POST crea mascotas y GET devuelve su listado.

### E3
5. BCrypt es un hash unidireccional con sal y costo de cálculo. No tiene una operación para
   descifrar y recuperar la clave original. PasswordEncoder.matches verifica la clave ingresada.
6. Permitir elegir el rol en un registro público permitiría crear cuentas ADMIN sin autorización.
   El backend fija USER e ignora el rol que intente enviar el cliente.

### E4
7. Un JWT contiene header, payload y firma. El payload incluye datos como identidad y expiración;
   la firma permite detectar alteraciones. Firmar no oculta el contenido y no debe incluir claves.
8. Registrar crea y guarda una cuenta con el hash de su clave. Iniciar sesión verifica las
   credenciales de una cuenta existente y emite el JWT si son correctas.
