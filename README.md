# security-service · LogiTrack

Microservicio de **usuarios, autenticación y emisión de JWT** de LogiTrack (Desarrollo y Arquitecturas
Avanzadas de Software, FI UNJu 2026). Registra usuarios con rol (`ADMIN`, `CLIENTE`, `REPARTIDOR`), valida el
login y entrega el token que `shipment-service` y `courier-service` verifican.

| | |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.0.9 · Spring Cloud 2025.1.1 |
| Puerto | `8083` (solo red interna) |
| Base de datos | MySQL 8.4 (H2 en memoria para tests) |
| Contraseñas | BCrypt, costo 12, sal aleatoria por hash |
| Tokens | JWT firmado con HS256 |

## Cifrado y seguridad

- **Contraseñas:** nunca se guardan en claro. Se almacenan con BCrypt (costo 12, configurable con
  `security.password.bcrypt-strength`). El costo y la sal viajan dentro del hash.
- **Política de contraseña:** entre 8 y 72 caracteres (BCrypt solo procesa 72 bytes).
- **Login:** responde `401 Credenciales inválidas` tanto si el usuario no existe como si la contraseña es
  incorrecta, y tarda lo mismo en los dos casos (se compara contra un hash ficticio), para no revelar qué
  cuentas existen.
- **JWT:** firmado con HS256. Lleva `sub` (id del usuario), `roles`, `repartidorId` (solo REPARTIDOR), `iat` y
  `exp`. El secreto **no tiene valor por defecto**: si falta `JWT_SECRET` o tiene menos de 32 caracteres, el
  servicio no arranca.
- **Sin sesión:** la aplicación es stateless; la identidad viaja solo en el token.
- **Actuator:** solo `health` e `info` son públicos.

### Generar un `JWT_SECRET` seguro

```bash
openssl rand -base64 48
```

En PowerShell (sin `openssl`):

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

El mismo valor tiene que usarse en `security-service`, `shipment-service` y `courier-service`.

## Variables de entorno

| Variable | Para qué |
|---|---|
| `DB_USER` / `DB_PASSWORD` | Credenciales de MySQL |
| `JWT_SECRET` | Clave de firma de los JWT (mínimo 32 caracteres, obligatoria) |
| `CONFIG_SERVER_URL` | URL del config server (por defecto `http://localhost:8888`) |
| `SPRING_PROFILES_ACTIVE` | `dev` (IDE) o `prod` (Docker) |

> Desde la PC no se accede directo: en Docker este puerto no se publica y todo entra por el `api-gateway` (`http://localhost:8066`), que enruta por Eureka.

## Ejecutar

```bash
./mvnw -B clean verify          # compila y corre los tests (perfil test: H2, sin MySQL ni config server)
./mvnw spring-boot:run          # requiere configserver y MySQL levantados
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Alta de usuario; devuelve el JWT |
| POST | `/api/v1/auth/login` | Login por usuario o email; devuelve el JWT |
| GET | `/api/v1/auth/me` | Datos del usuario del token |

## Docker

```bash
docker build -t macostag26/logitrack-security-service:dev .
```
