# Patitas al Rescate

API individual E1-E4: mascotas, registro con BCrypt y emisión de JWT.
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

Importar postman/Patitas_Individual.postman_collection.json y ejecutar en orden.
La variable baseUrl vale http://localhost:8080. No se requiere Bearer Token en esta fase.
Repetir el registro con el mismo username devuelve 409; para otra cuenta cambiar username
tanto en el registro como en el login.

- POST /api/mascotas: nombre, especie y edad. Responde 201. Estado siempre DISPONIBLE.
- GET /api/mascotas: responde 200 con los datos guardados.
- POST /api/auth/registrar: username y password. Responde 201 sin password, rol USER.
- POST /api/auth/login: username y password. Responde 200 con token o 401 genérico.

En pgAdmin, seleccionar patitas_db y ejecutar:

    SELECT * FROM public.mascotas ORDER BY id;
    SELECT id, username, password, rol FROM public.usuarios ORDER BY id;

El cliente no puede asignar rol ADMIN ni modificar mascotas existentes enviando un id al crear.

## JWT y alcance

JWT firmado con HS256, sujeto username, claim rol y expiración de 30 minutos.
Sin JWT_SECRET se genera una clave aleatoria en cada arranque, no publicada ni persistida.
Opcionalmente definir JWT_SECRET mediante entorno (al menos 32 bytes).
En esta entrega no hay filtro JWT, restricciones por rol, perfil ni logout/blacklist.
Los endpoints solicitados son públicos expresamente para el alcance individual.

## Comprobación automática

    mvnw.cmd clean verify

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
