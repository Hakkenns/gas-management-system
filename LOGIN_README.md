# 🔐 SISTEMA DE LOGIN - INSTRUCCIONES

## ✅ Cambios Realizados

Se ha implementado completamente el sistema de autenticación:

### 1. **AuthService** ✅
- Servicio de autenticación que valida username y password
- Usa BCrypt para comparar contraseñas
- Maneja usuarios en texto plano (legacy) y encriptados

### 2. **LoginController** ✅
- Maneja GET `/login` → muestra formulario
- Maneja POST `/login` → procesa autenticación
- Maneja GET `/logout` → cierra sesión

### 3. **login.html & login.js** ✅
- HTML con formulario AJAX
- JavaScript con validación y manejo de errores
- Redirige a dashboard después de login exitoso

### 4. **SecurityInterceptor** ✅
- Protege todas las rutas excepto `/login` y `/assets/`
- Redirige a login si no hay usuario en sesión

### 5. **Encriptación** ✅
- BCryptPasswordEncoder para contraseñas nuevas
- Fallback a comparación en plano para usuarios existentes

---

## 🔑 CREDENCIALES DE ACCESO

### Usuario de Prueba (Predefinido)
```
Usuario: admin
Contraseña: admin123
```

### Cómo acceder:
1. Abre: `http://localhost:8080`
2. Te redirige automáticamente a `/login`
3. Ingresa usuario y contraseña
4. Haz clic en "Ingresar"
5. Se te redirige al dashboard

---

## 🗄️ CREAR MÁS USUARIOS

### Opción 1: Manualmente desde el módulo de Usuarios (Recomendado)
1. Lógueate con `admin / admin123`
2. Ve a **Usuarios** (si tienes permisos)
3. Crea nuevo usuario con username, contraseña y perfil
4. La contraseña se encriptará automáticamente con BCrypt

### Opción 2: Desde Base de Datos (SQL)
```sql
-- Paso 1: Asegurate de que existe un Perfil
INSERT INTO perfiles (nombre_perfil, descripcion, estado) 
VALUES ('Usuario', 'Perfil de usuario normal', 1);

-- Paso 2: Asegurate de que existe un Empleado
INSERT INTO empleados (nombre, estado) 
VALUES ('Nombre Empleado', 1);

-- Paso 3: Inserta usuario con contraseña encriptada
-- Primero, genera un hash BCrypt desde: https://bcrypt-generator.com/
-- Ejemplo: "micontraseña" en BCrypt es: $2a$10$F9w7uLwUUZ1u...

INSERT INTO usuarios (username, password, correo, id_perfil, id_empleado, estado, fecha_creacion) 
VALUES (
    'nuevo_usuario',
    '$2a$10$F9w7uLwUUZ1u...',  -- REEMPLAZA ESTO CON TU HASH BCRYPT
    'usuario@ejemplo.com',
    1,  -- ID del perfil
    1,  -- ID del empleado
    1,
    NOW()
);
```

### Generador de contraseñas BCrypt:
→ https://bcrypt-generator.com/

---

## 🔄 FLUJO DE AUTENTICACIÓN

```
1. Usuario accede a http://localhost:8080/
   ↓
2. SecurityInterceptor verifica sesión
   ↓
3. Si NO hay sesión → redirige a /login
   ↓
4. Se muestra login.html
   ↓
5. Usuario ingresa credenciales y hace click
   ↓
6. login.js envía POST a /login vía AJAX
   ↓
7. LoginController recibe y llama a AuthService
   ↓
8. AuthService valida:
   - ¿Existe usuario?
   - ¿Está activo?
   - ¿Contraseña correcta?
   ↓
9. Si ✅ → Crea sesión y retorna datos
   ↓
10. JS redirige a / (dashboard)
    ↓
11. SecurityInterceptor verifica sesión ✅
    ↓
12. Se muestra dashboard
```

---

## ⚠️ NOTAS IMPORTANTES

### Contraseñas Existentes
- Si tuviste usuarios creados antes de esta implementación, sus contraseñas pueden estar en **texto plano**
- El AuthService tiene fallback para comparar en plano: `usuario.getPassword().equals(request.password())`
- **Se recomienda actualizar contraseñas existentes a BCrypt**

### Para encriptar contraseña existente (admin):
```sql
-- Actualiza el usuario con contraseña encriptada
UPDATE usuarios 
SET password = '$2a$10$slYQmyNdGzin7olVN3p5Be7DQH0B8Z9ff8F8rvWgJ1SMKPpjLfqDi'
WHERE username = 'admin';
```

### Estructura de Tabla de Usuarios (verificar)
```sql
CREATE TABLE usuarios (
    id_usuario BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    correo VARCHAR(150) UNIQUE NOT NULL,
    fecha_creacion DATETIME,
    estado INT DEFAULT 1,
    id_perfil BIGINT NOT NULL,
    id_empleado BIGINT,
    FOREIGN KEY (id_perfil) REFERENCES perfiles(id_perfil),
    FOREIGN KEY (id_empleado) REFERENCES empleados(id)
);
```

---

## 🐛 SOLUCIONAR PROBLEMAS

### ❌ El login no funciona
1. Verifica que hay un usuario en BD con el username
2. Verifica que el usuario tenga `estado = 1`
3. Abre la consola del navegador (F12) y revisa errores
4. Revisa logs de Spring: `tail -f logs/spring.log`

### ❌ No me redirige al dashboard
- Verifica que DashboardController existe y tiene ruta `/`
- Verifica que el interceptor no está bloqueando `/`

### ❌ Contraseña rechazada
- Verifica que la contraseña en BD está encriptada con BCrypt
- O usa texto plano temporalmente
- Usa https://bcrypt-generator.com/ para generar hash correcto

### ❌ Las rutas están bloqueadas sin login
- Eso es CORRECTO, es la seguridad del interceptor
- Solo `/login` y `/assets/` son públicas

---

## 📝 ARCHIVOS MODIFICADOS/CREADOS

✅ `/src/main/java/com/gas/sistema_gas/service/AuthService.java` - Interface
✅ `/src/main/java/com/gas/sistema_gas/service/Implement/AuthServiceImplement.java` - Implementación
✅ `/src/main/java/com/gas/sistema_gas/controller/LoginController.java` - Ya existía, actualizado
✅ `/src/main/java/com/gas/sistema_gas/config/SecurityInterceptor.java` - Nuevo
✅ `/src/main/java/com/gas/sistema_gas/config/WebConfig.java` - Nuevo
✅ `/src/main/resources/templates/views/login.html` - Actualizado
✅ `/src/main/resources/static/assets/js/login.js` - Actualizado
✅ `/src/main/resources/sql/usuarios_prueba.sql` - Script de datos

---

## ✨ NEXT STEPS

1. **Ejecutar la aplicación**
   ```bash
   mvn spring-boot:run
   ```

2. **Ejecutar script SQL** (en tu BD):
   ```sql
   -- Abre en tu cliente MySQL el archivo: src/main/resources/sql/usuarios_prueba.sql
   ```

3. **Acceder a**: `http://localhost:8080`

4. **Ingresar con**: `admin / admin123`

---

¿Dudas? 🤔
