-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3307
-- Tiempo de generación: 01-08-2026 a las 01:52:41
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `sistema_gas`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `asignacion_motos`
--

CREATE TABLE `asignacion_motos` (
  `id_asignacion` bigint(20) NOT NULL,
  `id_moto` bigint(20) NOT NULL,
  `id_empleado` bigint(20) NOT NULL,
  `fecha_asignacion` datetime NOT NULL,
  `fecha_devolucion` datetime DEFAULT NULL,
  `estado` enum('ACTIVA','FINALIZADA') NOT NULL DEFAULT 'ACTIVA',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `asignacion_motos`
--

INSERT INTO `asignacion_motos` (`id_asignacion`, `id_moto`, `id_empleado`, `fecha_asignacion`, `fecha_devolucion`, `estado`, `created_at`, `updated_at`) VALUES
(1, 6, 3, '2026-07-20 18:07:23', '2026-07-20 19:50:34', 'FINALIZADA', '2026-07-20 18:07:23', '2026-07-20 19:50:34'),
(2, 1, 7, '2026-07-20 18:07:46', '2026-07-25 01:45:52', 'FINALIZADA', '2026-07-20 18:07:46', '2026-07-25 01:45:52'),
(3, 4, 3, '2026-07-20 19:50:34', '2026-07-20 22:55:33', 'FINALIZADA', '2026-07-20 19:50:34', '2026-07-20 22:55:33'),
(4, 7, 3, '2026-07-20 22:55:33', '2026-07-21 04:58:19', 'FINALIZADA', '2026-07-20 22:55:33', '2026-07-21 04:58:19'),
(5, 4, 3, '2026-07-21 04:58:19', NULL, 'ACTIVA', '2026-07-21 04:58:19', '2026-07-21 04:58:19'),
(6, 8, 7, '2026-07-25 01:45:52', NULL, 'ACTIVA', '2026-07-25 01:45:52', '2026-07-25 01:45:52');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `catalogo_proveedores`
--

CREATE TABLE `catalogo_proveedores` (
  `id_catalogo` bigint(20) NOT NULL,
  `id_proveedor` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `precio_producto_proveedor` decimal(10,2) DEFAULT NULL COMMENT 'Precio por contenido',
  `precio_envase_proveedor` decimal(10,2) DEFAULT NULL COMMENT 'Precio por envase',
  `es_activo` tinyint(1) DEFAULT 1 COMMENT 'Catálogo vigente'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `catalogo_proveedores`
--

INSERT INTO `catalogo_proveedores` (`id_catalogo`, `id_proveedor`, `id_producto`, `created_at`, `precio_producto_proveedor`, `precio_envase_proveedor`, `es_activo`) VALUES
(17, 6, 1, '2026-07-20 18:30:57', NULL, NULL, 1),
(18, 3, 2, '2026-07-20 18:31:01', NULL, NULL, 1),
(19, 4, 3, '2026-07-20 18:31:08', NULL, NULL, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categorias`
--

CREATE TABLE `categorias` (
  `id_categoria` bigint(20) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `unidad_medida` varchar(10) NOT NULL DEFAULT 'UND',
  `requiere_capacidad` tinyint(1) NOT NULL DEFAULT 0,
  `etiqueta_capacidad` varchar(50) DEFAULT NULL,
  `maneja_envase` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `categorias`
--

INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`, `created_at`, `updated_at`, `unidad_medida`, `requiere_capacidad`, `etiqueta_capacidad`, `maneja_envase`) VALUES
(1, 'Gas Doméstico', 'balones de gas GLP para uso doméstico en hogares, cocinas y pequeños negocios.', 1, '2026-05-15 06:25:19', '2026-06-18 18:06:03', 'KG', 1, 'Capacidad (kg)', 1),
(2, 'Accesorios(Por Unidad)', 'productos complementarios para la instalación y seguridad del sistema de gas, como reguladores, mangueras y abrazaderas.', 1, '2026-05-15 06:26:14', '2026-06-10 18:32:30', 'UND', 0, NULL, 0),
(3, 'Bidones de Agua', 'bidones de agua para consumo doméstico y comercial, en diferentes capacidades y presentaciones', 1, '2026-05-15 09:23:07', '2026-06-10 17:27:17', 'L', 1, 'Contenido (Litros)', 1),
(4, 'Accesorios(Por Metros)', '', 1, '2026-06-10 18:32:54', '2026-06-10 18:32:54', 'M', 1, 'Longitud (Metros)', 0),
(5, 'Aguita', 'hola', 1, '2026-07-20 16:14:19', '2026-07-20 16:14:19', 'L', 1, 'Contenido (Litros)', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categoria_capacidades`
--

CREATE TABLE `categoria_capacidades` (
  `id_capacidad` bigint(20) NOT NULL,
  `valor_capacidad` decimal(5,2) NOT NULL,
  `id_categoria` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `categoria_capacidades`
--

INSERT INTO `categoria_capacidades` (`id_capacidad`, `valor_capacidad`, `id_categoria`) VALUES
(1, 15.00, 5),
(2, 20.00, 5),
(3, 10.00, 1),
(4, 50.00, 4),
(5, 100.00, 4);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `clientes`
--

CREATE TABLE `clientes` (
  `id_cliente` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `dni` varchar(8) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `referencia` varchar(150) DEFAULT NULL,
  `telefono` varchar(9) DEFAULT NULL,
  `correo` varchar(255) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `latitud` double DEFAULT NULL,
  `longitud` double DEFAULT NULL,
  `tipo_cliente` enum('PARTICULAR','COMERCIAL','MAYORISTA') DEFAULT 'PARTICULAR' COMMENT 'Clasificación del cliente',
  `permite_prestamo` tinyint(1) DEFAULT 0 COMMENT 'Puede tomar préstamos de balones',
  `prestamo_ilimitado` tinyint(1) DEFAULT 0 COMMENT 'Para comercios: permite préstamos indefinidos',
  `limite_deuda_balones` int(11) DEFAULT 1 COMMENT 'Máximo de balones que puede deber',
  `es_deudor_activo` tinyint(1) DEFAULT 0 COMMENT 'Flag rápido: tiene deuda activa'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `clientes`
--

INSERT INTO `clientes` (`id_cliente`, `nombre`, `dni`, `direccion`, `referencia`, `telefono`, `correo`, `estado`, `created_at`, `updated_at`, `latitud`, `longitud`, `tipo_cliente`, `permite_prestamo`, `prestamo_ilimitado`, `limite_deuda_balones`, `es_deudor_activo`) VALUES
(1, 'ROY AGAPITO VEGAS', '73838677', 'AV. Pedro Cieza de León 675', 'Frente al colegio Virgen de la Paz, Chiclayo, Peru', '963335241', 'royagapito248@gmail.com', 1, '2026-06-05 18:47:07', '2026-07-16 19:31:37', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(3, 'AURISTELA GUZMAN LLANOS', '45821934', 'Av. Balta 1210', 'A media cuadra del Parque Principal, al costado del banco BCP', '963335548', 'Guzman@gmail.com', 1, '2026-06-09 06:07:57', '2026-07-15 22:44:39', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(4, 'JUAN ZARATE PAUCARCAJA', '44558663', 'Calle San José 815', 'Frente a la Plazuela Elías Aguirre, Chiclayo, Perú', '963335336', 'correo@gmail.com', 1, '2026-06-09 06:08:43', '2026-07-16 18:40:15', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(6, 'MARICIELO TORRES RAMIREZ', '72145896', 'Av. Luis Gonzales 650', 'Frente al Mercado Modelo, cerca a la cochera', '965888524', 'Maricielo@gmail.com', 1, '2026-06-09 08:52:04', '2026-07-15 22:45:15', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(7, 'NORMA BAZAN OJANAMA', '10284753', 'Av. Salaverry 820', 'Frente al Real Plaza, por la entrada peatonal', '938545399', 'Norma@gmail.com', 1, '2026-06-09 09:17:17', '2026-07-15 22:45:56', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(8, 'CARLOS QUISPE ALEJOS', '10203040', 'Av. Balta 123', 'Frente al parque principal', '965523645', 'carlos.mendoza@test.com', 1, '2026-07-17 15:29:39', '2026-07-17 15:29:39', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(9, 'LUIS HUARCAYA TORRES', '72145698', 'Calle San José 345', 'A media cuadra del óvalo', '955526488', 'ana.gomez@test.com', 1, '2026-07-17 15:30:17', '2026-07-17 15:30:17', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(10, 'EDUARDO SORIANO SAENZ', '09874521', 'Av. Grau 560', 'Frente al grifo', '953265444', 'luis.castro@test.com', 1, '2026-07-17 15:30:50', '2026-07-17 15:30:50', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(11, 'EDDY SANCHEZ MONTALBAN', '47521896', 'Av. Salaverry 410', 'A la espalda del mercado', '952202024', 'miguel.benites@test.com', 1, '2026-07-17 15:32:24', '2026-07-17 15:32:24', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(12, 'AURISTELA JOSEFINA CAROLINA GUZMAN LLANOS', NULL, 'Av. Luis Gonzales 650', 'A la espalda del mercado', '963335336', NULL, 1, '2026-07-20 16:00:06', '2026-07-20 16:00:06', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(13, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Salaverry 820', 'Frente al Real Plaza, por la entrada peatonal', '965888524', NULL, 1, '2026-07-20 16:03:25', '2026-07-20 16:03:25', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(15, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Salaverry 820', 'Frente al colegio Virgen de la Paz, Chiclayo, Peru', '952202024', NULL, 1, '2026-07-20 16:04:05', '2026-07-20 16:04:05', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(18, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Salaverry 820', 'ASDASDA', '938545399', NULL, 1, '2026-07-20 17:01:38', '2026-07-20 17:01:38', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(19, 'MARICIELO GIOVANNA TORRES RAMIREZ', NULL, 'ADSASD', 'Frente al Real Plaza, por la entrada peatonal', '963335336', NULL, 1, '2026-07-20 17:08:10', '2026-07-20 17:08:10', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(20, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Salaverry 820', 'Frente al Mercado Modelo, cerca a la cochera', '938545399', NULL, 1, '2026-07-20 17:18:57', '2026-07-20 17:18:57', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(22, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Luis Gonzales 650', 'Frente al Real Plaza, por la entrada peatonal', '', NULL, 1, '2026-07-20 17:43:24', '2026-07-20 17:43:24', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(23, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Luis Gonzales 650', 'Frente al Mercado Modelo, cerca a la cochera', '938545399', NULL, 1, '2026-07-20 17:47:23', '2026-07-20 17:47:23', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(24, 'NORMA LUZ BAZAN OJANAMA', NULL, 'Av. Salaverry 820', 'Frente a la Plazuela Elías Aguirre, Chiclayo, Perú', '963335336', NULL, 1, '2026-07-20 17:53:30', '2026-07-20 17:53:30', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(25, 'MARICIELO GIOVANNA TORRES RAMIREZ', NULL, 'Av. Luis Gonzales 650', 'Frente al Mercado Modelo, cerca a la cochera', '938545399', NULL, 1, '2026-07-20 17:56:42', '2026-07-20 17:56:42', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(26, 'MARICIELO GIOVANNA TORRES RAMIREZ', NULL, 'ADSASD', 'Frente al Real Plaza, por la entrada peatonal', '963335241', NULL, 1, '2026-07-20 17:58:19', '2026-07-20 17:58:19', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(28, 'ROY ESNAYDEHER AGAPITO VEGAS', NULL, 'ADSASD', 'Frente a la Plazuela Elías Aguirre, Chiclayo, Perú', '938545399', NULL, 1, '2026-07-20 19:49:34', '2026-07-20 19:49:34', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(29, 'ROY ESNAYDEHER AGAPITO VEGAS', NULL, 'ADSASD', 'ASDASDA', '938545399', NULL, 1, '2026-07-20 20:41:44', '2026-07-20 20:41:44', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0),
(30, 'JUAN MARCOS ZARATE PAUCARCAJA', NULL, 'ADSASD', 'Frente a la Plazuela Elías Aguirre, Chiclayo, Perú', '938545399', NULL, 1, '2026-07-20 21:29:58', '2026-07-20 21:29:58', NULL, NULL, 'PARTICULAR', 0, 0, 1, 0);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `compras`
--

CREATE TABLE `compras` (
  `id_compra` bigint(20) NOT NULL,
  `id_proveedor` bigint(20) NOT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `fecha_compra` datetime NOT NULL,
  `num_documento` varchar(20) DEFAULT NULL,
  `monto_total` decimal(12,2) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `situacion` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `compras`
--

INSERT INTO `compras` (`id_compra`, `id_proveedor`, `id_usuario`, `fecha_compra`, `num_documento`, `monto_total`, `created_at`, `updated_at`, `situacion`) VALUES
(1, 3, 2, '2026-07-20 19:48:00', 'NC001-0048', 400.00, '2026-07-20 19:48:55', '2026-07-20 19:48:55', 1),
(2, 6, 2, '2026-07-21 04:46:00', 'NC001-0049', 20.00, '2026-07-21 04:47:03', '2026-07-21 04:47:03', 1),
(3, 6, 2, '2026-07-21 04:55:00', 'NC001-0050', 200.00, '2026-07-21 04:55:16', '2026-07-21 04:55:16', 1),
(4, 3, 2, '2026-07-25 01:40:00', 'NC001-0051', 500.00, '2026-07-25 01:40:44', '2026-07-25 01:40:44', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `configuracion`
--

CREATE TABLE `configuracion` (
  `id` bigint(20) NOT NULL,
  `nombre_empresa` varchar(150) NOT NULL,
  `razon_social` varchar(200) DEFAULT NULL,
  `ruc` char(11) DEFAULT NULL,
  `direccion` varchar(250) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `correo` varchar(120) DEFAULT NULL,
  `logo` varchar(255) DEFAULT NULL,
  `moneda` varchar(10) DEFAULT 'PEN',
  `porcentaje_igv` decimal(5,2) DEFAULT 18.00,
  `serie_factura` varchar(10) DEFAULT NULL,
  `serie_boleta` varchar(10) DEFAULT NULL,
  `mensaje_comprobante` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1,
  `fecha_creacion` timestamp NOT NULL DEFAULT current_timestamp(),
  `fecha_actualizacion` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `control_envase`
--

CREATE TABLE `control_envase` (
  `id_control` bigint(20) NOT NULL,
  `cantidad_prestada` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `fecha_devolucion` datetime(6) DEFAULT NULL,
  `fecha_prestamo` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `id_cliente` bigint(20) NOT NULL,
  `id_pedido` bigint(20) DEFAULT NULL,
  `id_producto` bigint(20) DEFAULT NULL,
  `cantidad_devuelta` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `control_envase`
--

INSERT INTO `control_envase` (`id_control`, `cantidad_prestada`, `created_at`, `estado`, `fecha_devolucion`, `fecha_prestamo`, `updated_at`, `id_cliente`, `id_pedido`, `id_producto`, `cantidad_devuelta`) VALUES
(1, 1, '2026-07-20 20:41:44.000000', 'PRESTADO', NULL, '2026-07-20 20:41:44.000000', '2026-07-20 20:41:44.000000', 29, 2, 2, 0),
(2, 1, '2026-07-20 22:17:06.000000', 'PRESTADO', '2026-07-22 00:00:00.000000', '2026-07-20 22:17:06.000000', '2026-07-20 22:17:06.000000', 6, 7, 2, 0),
(3, 1, '2026-07-20 22:37:11.000000', 'SALDADO', '2026-07-21 05:01:05.000000', '2026-07-20 22:37:11.000000', '2026-07-21 05:01:05.000000', 4, 9, 2, 1),
(4, 1, '2026-07-20 22:47:55.000000', 'SALDADO', '2026-07-21 01:00:23.000000', '2026-07-20 22:47:55.000000', '2026-07-21 01:00:23.000000', 10, 11, 2, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `correlativos`
--

CREATE TABLE `correlativos` (
  `id_correlativo` int(11) NOT NULL,
  `numero_actual` int(11) NOT NULL,
  `serie` varchar(10) NOT NULL,
  `tipo` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `correlativos`
--

INSERT INTO `correlativos` (`id_correlativo`, `numero_actual`, `serie`, `tipo`) VALUES
(1, 49, 'NV001', 'VENTA_NOTA'),
(2, 51, 'NC001', 'COMPRA_NOTA');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalle_compra`
--

CREATE TABLE `detalle_compra` (
  `id_detalle_compra` bigint(20) NOT NULL,
  `id_compra` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_costo_unitario` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `detalle_compra`
--

INSERT INTO `detalle_compra` (`id_detalle_compra`, `id_compra`, `id_producto`, `cantidad`, `precio_costo_unitario`) VALUES
(1, 1, 2, 10, 40.00),
(2, 2, 1, 1, 20.00),
(3, 3, 1, 10, 20.00),
(4, 4, 2, 10, 50.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalle_pedido`
--

CREATE TABLE `detalle_pedido` (
  `id_detalle` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL,
  `id_pedido` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `detalle_pedido`
--

INSERT INTO `detalle_pedido` (`id_detalle`, `cantidad`, `precio_unitario`, `id_pedido`, `id_producto`) VALUES
(1, 1, 50.00, 1, 2),
(2, 1, 50.00, 2, 2),
(3, 1, 50.00, 3, 2),
(4, 1, 50.00, 8, 2),
(5, 1, 50.00, 10, 2),
(6, 1, 50.00, 12, 2),
(7, 1, 150.00, 13, 2),
(8, 1, 30.00, 15, 1),
(9, 1, 10.00, 15, 1),
(10, 1, 50.00, 16, 2),
(11, 1, 100.00, 16, 2),
(12, 1, 50.00, 17, 2),
(13, 1, 100.00, 17, 2),
(14, 1, 60.00, 18, 2),
(15, 1, 60.00, 19, 2),
(16, 1, 60.00, 20, 2);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `deuda_envases`
--

CREATE TABLE `deuda_envases` (
  `id_deuda` bigint(20) NOT NULL,
  `id_cliente` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `cantidad_adeudada` int(11) NOT NULL,
  `motivo` enum('PRESTAMO_INCUMPLIDO','INTERCAMBIO_PENDIENTE','OTROS') NOT NULL,
  `fecha_generacion_deuda` datetime NOT NULL DEFAULT current_timestamp(),
  `estado` enum('ACTIVA','PARCIALMENTE_PAGADA','SALDADA') DEFAULT 'ACTIVA',
  `fecha_saldo` datetime DEFAULT NULL,
  `notas` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `empleados`
--

CREATE TABLE `empleados` (
  `id_empleado` bigint(20) NOT NULL,
  `dni` varchar(8) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `telefono` varchar(9) NOT NULL,
  `sueldo_base` decimal(10,2) NOT NULL DEFAULT 0.00,
  `descuentos` decimal(10,2) NOT NULL DEFAULT 0.00,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `correo` varchar(255) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `empleados`
--

INSERT INTO `empleados` (`id_empleado`, `dni`, `nombre`, `telefono`, `sueldo_base`, `descuentos`, `estado`, `created_at`, `updated_at`, `correo`, `direccion`) VALUES
(2, '70135060', 'YOVANA MAMANI FAIJO', '965456456', 150.00, 0.00, 1, '2026-06-06 19:33:26', '2026-06-06 19:47:05', NULL, NULL),
(3, '73838677', 'ROY AGAPITO VEGAS', '963332114', 122.00, 0.00, 1, '2026-06-06 19:41:08', '2026-07-16 22:38:25', 'roy1@gmail.com', NULL),
(4, '76195535', 'ALEX AGAPITO VEGAS', '963332166', 250.00, 0.00, 0, '2026-06-06 20:28:56', '2026-06-09 05:03:15', NULL, NULL),
(5, '71513386', 'KEMJI ENEQUE APAZA', '969696969', 6000.00, 0.00, 1, '2026-06-06 23:56:18', '2026-06-06 23:56:18', NULL, NULL),
(6, '75142854', 'ABEL ORDOÑEZ ZAPATA', '938545399', 1200.00, 0.00, 1, '2026-06-09 08:48:29', '2026-06-09 08:48:29', NULL, NULL),
(7, '75926849', 'LEIDY BRUNO CHAVEZ', '954826111', 1000.00, 0.00, 1, '2026-06-09 09:09:50', '2026-07-14 14:52:24', 'Leydy1234@gmail.com', '');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `envases`
--

CREATE TABLE `envases` (
  `id` bigint(20) NOT NULL,
  `capacidad` decimal(10,2) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` bit(1) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `stock_inicial` int(11) NOT NULL,
  `unidad_medida` varchar(20) NOT NULL,
  `precio_envase` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `envases`
--

INSERT INTO `envases` (`id`, `capacidad`, `descripcion`, `estado`, `nombre`, `stock_inicial`, `unidad_medida`, `precio_envase`) VALUES
(1, 10.00, NULL, b'1', 'Balón de gas', 0, 'KG', 100.00),
(2, 20.00, NULL, b'1', 'Bidón de agua', 0, 'L', 10.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `evidencias`
--

CREATE TABLE `evidencias` (
  `id_evidencia` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `tipo_evidencia` varchar(50) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `url_imagen` varchar(255) NOT NULL,
  `id_pago` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `evidencias`
--

INSERT INTO `evidencias` (`id_evidencia`, `created_at`, `tipo_evidencia`, `updated_at`, `url_imagen`, `id_pago`) VALUES
(1, '2026-07-21 01:42:54', 'PAGO', '2026-07-21 01:42:54', '/imagenes-sistema/8a1a83e4-6af7-4d81-a818-184a3131024a.jpg', 1),
(2, '2026-07-21 02:33:37', 'PAGO', '2026-07-21 02:33:37', '/imagenes-sistema/02ee10bb-2580-4c35-a726-f1253c72a6c5.jpg', 2),
(3, '2026-07-21 03:41:10', 'PAGO', '2026-07-21 03:41:10', '/imagenes-sistema/024a968a-c3b1-4f7e-a288-fd7bce603b8b.jpg', 3),
(4, '2026-07-21 03:46:55', 'PAGO', '2026-07-21 03:46:55', '/imagenes-sistema/545c235d-0bfb-4678-a16a-8c1cd884011c.png', 4),
(5, '2026-07-21 08:43:55', 'PAGO', '2026-07-21 08:43:55', '/imagenes-sistema/18500aea-3729-4660-b7cc-caa9a7e1bf41.png', 5),
(6, '2026-07-21 08:43:55', 'PAGO', '2026-07-21 08:43:55', '/imagenes-sistema/41b5ec87-b291-412d-95cd-6744a6e91922.png', 6),
(7, '2026-07-21 08:43:55', 'PAGO', '2026-07-21 08:43:55', '/imagenes-sistema/d4dc1ed4-0682-47f6-9a35-c4214813b5fe.png', 7),
(8, '2026-07-21 08:43:55', 'VUELTO', '2026-07-21 08:43:55', '/imagenes-sistema/d7ecab18-59fe-48ec-8f12-f7ae037ea21b.png', 7),
(9, '2026-07-21 09:56:09', 'PAGO', '2026-07-21 09:56:09', '/imagenes-sistema/414469e4-ed24-4e84-a1b9-6c95db631e5a.webp', 8),
(10, '2026-07-21 09:57:41', 'PAGO', '2026-07-21 09:57:41', '/imagenes-sistema/4ae49b40-290c-480b-bfd9-8a1af6daacfd.jpg', 9),
(11, '2026-07-25 06:39:09', 'PAGO', '2026-07-25 06:39:09', '/imagenes-sistema/19c360ef-37df-4112-bf9a-41c3f894c658.png', 10),
(12, '2026-07-25 06:45:19', 'PAGO', '2026-07-25 06:45:19', '/imagenes-sistema/b83f0713-7b33-404e-b655-4165c11c0364.png', 11),
(13, '2026-07-25 06:45:19', 'PAGO', '2026-07-25 06:45:19', '/imagenes-sistema/2467af92-9420-41c7-a01d-b52febc89629.png', 12),
(14, '2026-07-29 03:52:21', 'PAGO', '2026-07-29 03:52:21', '/imagenes-sistema/0b918622-7c04-458a-9f0d-af230fd7db8c.jpg', 13),
(15, '2026-07-29 03:52:21', 'PAGO', '2026-07-29 03:52:21', '/imagenes-sistema/7f17c746-af51-4b86-b4f9-ea7da854070b.jpg', 14),
(16, '2026-07-29 03:52:21', 'PAGO', '2026-07-29 03:52:21', '/imagenes-sistema/ec460b6f-2ecd-45b6-85a7-eedb80ef0d21.jpg', 15),
(17, '2026-07-29 03:52:21', 'VUELTO', '2026-07-29 03:52:21', '/imagenes-sistema/2c7723aa-b543-4461-8176-203669453fb9.jpg', 15);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `gastos_operativos_caja_chica`
--

CREATE TABLE `gastos_operativos_caja_chica` (
  `id_gasto` bigint(20) NOT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `concepto` varchar(150) NOT NULL,
  `monto` decimal(10,2) NOT NULL,
  `categoria` enum('MANTENIMIENTO','COMBUSTIBLE','COMPRAS_VARIAS','OTROS') NOT NULL,
  `fecha_gasto` datetime NOT NULL DEFAULT current_timestamp(),
  `descripcion` text DEFAULT NULL,
  `comprobante` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_stock`
--

CREATE TABLE `historial_stock` (
  `id_historial` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `fecha` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `id_producto` bigint(20) NOT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `tipo_movimiento` enum('AJUSTE','DEVOLUCION','ENTRADA_COMPRA','SALIDA_VENTA') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `incidencias`
--

CREATE TABLE `incidencias` (
  `id_incidencia` bigint(20) NOT NULL,
  `id_empleado` bigint(20) NOT NULL,
  `id_pedido` bigint(20) DEFAULT NULL,
  `tipo` varchar(50) NOT NULL,
  `tipo_label` varchar(100) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `tipo_incidencia` varchar(50) NOT NULL,
  `url_evidencia` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `incidencias`
--

INSERT INTO `incidencias` (`id_incidencia`, `id_empleado`, `id_pedido`, `tipo`, `tipo_label`, `estado`, `created_at`, `updated_at`, `tipo_incidencia`, `url_evidencia`) VALUES
(1, 3, NULL, 'CRITICA', 'Alerta Crítica', 'ATENDIDO', '2026-07-20 19:50:15', '2026-07-20 19:50:34', 'AVERIA_VEHICULO', NULL),
(2, 3, 1, 'WARNING', 'Advertencia de Entrega', 'CONFIRMADO', '2026-07-20 19:51:31', '2026-07-21 04:53:19', 'RECHAZO_DISTANCIA', NULL),
(3, 7, 1, 'INFO', 'Cliente Ausente', 'CONFIRMADO', '2026-07-20 19:52:49', '2026-07-21 04:53:19', 'CLIENTE_AUSENTE', '/imagenes-sistema/incidencia_1_532b66a6-7fa7-4c00-9d2c-1af9d9f648a1.jpg'),
(4, 3, NULL, 'CRITICA', 'Alerta Crítica', 'ATENDIDO', '2026-07-20 22:55:28', '2026-07-20 22:55:33', 'AVERIA_VEHICULO', NULL),
(5, 7, 1, 'INFO', 'Cliente Ausente', 'CONFIRMADO', '2026-07-21 00:59:03', '2026-07-21 04:53:19', 'CLIENTE_AUSENTE', '/imagenes-sistema/incidencia_1_840e6013-0270-4332-80f2-b9b6e018e6b1.jpg'),
(6, 3, 12, 'INFO', 'Cliente Ausente', 'ATENDIDO', '2026-07-21 04:51:59', '2026-07-21 04:52:02', 'CLIENTE_AUSENTE', '/imagenes-sistema/incidencia_12_b60b9080-4ef4-475d-8ccd-1ed716327640.png'),
(7, 3, 15, 'WARNING', 'Advertencia de Entrega', 'ATENDIDO', '2026-07-21 04:52:21', '2026-07-21 04:52:26', 'RECHAZO_DISTANCIA', NULL),
(8, 7, 1, 'WARNING', 'Rechazo de Pedido', 'CONFIRMADO', '2026-07-21 04:53:06', '2026-07-21 04:53:19', 'RECHAZO_POST_LLEGADA', '/imagenes-sistema/incidencia_1_010a0203-a9b1-4423-ae55-dfb4d18c51d4.webp'),
(9, 3, NULL, 'CRITICA', 'Alerta Crítica', 'ATENDIDO', '2026-07-21 04:58:14', '2026-07-21 04:58:19', 'AVERIA_VEHICULO', NULL),
(10, 3, 17, 'WARNING', 'Advertencia de Entrega', 'CONFIRMADO', '2026-07-21 10:39:56', '2026-07-21 10:43:38', 'RECHAZO_DISTANCIA', NULL),
(11, 7, 17, 'WARNING', 'Rechazo de Pedido', 'CONFIRMADO', '2026-07-21 10:43:20', '2026-07-21 10:43:38', 'RECHAZO_POST_LLEGADA', '/imagenes-sistema/incidencia_17_be82dcad-fd49-43b2-bef6-a80366aaad06.jpg'),
(12, 7, NULL, 'CRITICA', 'Alerta Crítica', 'ATENDIDO', '2026-07-25 01:45:47', '2026-07-25 01:45:52', 'AVERIA_VEHICULO', NULL),
(13, 7, 19, 'WARNING', 'Advertencia de Entrega', 'CONFIRMADO', '2026-07-25 01:46:18', '2026-07-25 01:47:44', 'RECHAZO_DISTANCIA', NULL),
(14, 3, 19, 'INFO', 'Cliente Ausente', 'CONFIRMADO', '2026-07-25 01:47:04', '2026-07-25 01:47:44', 'CLIENTE_AUSENTE', '/imagenes-sistema/incidencia_19_9d88e5fa-ba9c-4038-8bf8-93810a981066.png'),
(15, 3, 19, 'WARNING', 'Rechazo de Pedido', 'CONFIRMADO', '2026-07-25 01:47:31', '2026-07-25 01:47:44', 'RECHAZO_POST_LLEGADA', '/imagenes-sistema/incidencia_19_cbdaffbd-b1da-49bd-8cc9-45e481883de7.png');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario`
--

CREATE TABLE `inventario` (
  `id_inventario` bigint(20) NOT NULL,
  `stock_llenos` int(11) NOT NULL DEFAULT 0,
  `stock_vacios` int(11) NOT NULL DEFAULT 0,
  `id_producto` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario_estado`
--

CREATE TABLE `inventario_estado` (
  `id_estado` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `cantidad_llenos` int(11) NOT NULL DEFAULT 0,
  `cantidad_vacios` int(11) NOT NULL DEFAULT 0,
  `cantidad_en_prestamo` int(11) NOT NULL DEFAULT 0,
  `cantidad_vendidos` int(11) NOT NULL DEFAULT 0,
  `total_esperado` int(11) NOT NULL COMMENT 'Espacio total o cantidad total esperada',
  `fecha_cuadre` datetime NOT NULL DEFAULT current_timestamp(),
  `observaciones` text DEFAULT NULL,
  `id_usuario_cuadre` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario_lotes`
--

CREATE TABLE `inventario_lotes` (
  `id_lote` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `id_proveedor` bigint(20) NOT NULL,
  `cantidad_inicial` decimal(10,2) NOT NULL,
  `cantidad_actual` decimal(10,2) NOT NULL,
  `precio_compra` decimal(10,2) NOT NULL,
  `precio_venta` decimal(10,2) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `id_compra` bigint(20) DEFAULT NULL,
  `metros_por_rollo` decimal(10,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `inventario_lotes`
--

INSERT INTO `inventario_lotes` (`id_lote`, `id_producto`, `id_proveedor`, `cantidad_inicial`, `cantidad_actual`, `precio_compra`, `precio_venta`, `created_at`, `updated_at`, `id_compra`, `metros_por_rollo`) VALUES
(1, 2, 3, 10.00, 0.00, 40.00, 50.00, '2026-07-20 19:48:55', '2026-07-21 10:42:20', 1, NULL),
(2, 1, 6, 1.00, 0.00, 20.00, 30.00, '2026-07-21 04:47:03', '2026-07-21 04:55:33', 2, NULL),
(3, 1, 6, 10.00, 9.00, 20.00, 30.00, '2026-07-21 04:55:16', '2026-07-21 04:55:33', 3, NULL),
(4, 2, 3, 10.00, 8.00, 50.00, 60.00, '2026-07-25 01:40:44', '2026-07-28 22:49:01', 4, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `metas_venta`
--

CREATE TABLE `metas_venta` (
  `id_metas` bigint(20) NOT NULL,
  `id_empleado` bigint(20) NOT NULL,
  `anio` int(11) NOT NULL,
  `mes` int(11) NOT NULL,
  `obj_venta` decimal(10,2) NOT NULL,
  `bono_meta` decimal(10,2) NOT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `metodo_pago`
--

CREATE TABLE `metodo_pago` (
  `id_metodo` bigint(20) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `estado` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `metodo_pago`
--

INSERT INTO `metodo_pago` (`id_metodo`, `nombre`, `estado`) VALUES
(1, 'Efectivo', 1),
(2, 'Yape', 1),
(3, 'Plin', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `motos`
--

CREATE TABLE `motos` (
  `id_moto` bigint(20) NOT NULL,
  `placa` varchar(20) NOT NULL,
  `marca` varchar(100) NOT NULL,
  `modelo` varchar(100) NOT NULL,
  `anio` int(11) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `motos`
--

INSERT INTO `motos` (`id_moto`, `placa`, `marca`, `modelo`, `anio`, `estado`, `created_at`, `updated_at`) VALUES
(1, 'NGA2415', 'Honda', 'GL125', 2023, 1, '2026-06-05 20:40:08', '2026-06-05 20:48:27'),
(2, '5421MV', 'Bajaj', 'Pulsar NS150', 2026, 1, '2026-06-05 20:44:50', '2026-07-18 02:59:28'),
(3, 'NGA2445', 'Honda', 'GL300', 2016, 0, '2026-06-05 20:48:04', '2026-06-05 20:48:45'),
(4, '5425GHJ', 'Honda', 'Furius 3000', 2026, 1, '2026-06-05 21:04:36', '2026-06-05 21:04:36'),
(5, 'NGA2223', 'Bajaj', 'modelo1', 2011, 0, '2026-06-05 21:07:12', '2026-06-05 21:07:20'),
(6, '12345A', 'Honda', 'CB190R', 2024, 1, '2026-07-17 19:26:42', '2026-07-17 19:26:42'),
(7, '9876MX', 'Bajaj', 'Pulsar NS200', 2023, 1, '2026-07-17 19:27:07', '2026-07-17 19:27:07'),
(8, '54321B', 'Yamaha', 'FZ25', 2025, 1, '2026-07-17 19:27:28', '2026-07-17 19:27:28');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `opciones`
--

CREATE TABLE `opciones` (
  `id_opciones` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `icono` varchar(100) NOT NULL,
  `ruta` varchar(150) NOT NULL,
  `estado` int(11) NOT NULL,
  `id_padre` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `opciones`
--

INSERT INTO `opciones` (`id_opciones`, `nombre`, `icono`, `ruta`, `estado`, `id_padre`) VALUES
(1, 'Dashboard', 'fas fa-tachometer-alt', 'dashboard', 1, NULL),
(2, 'Perfiles', 'fas fa-user-shield', 'perfiles', 1, NULL),
(3, 'Usuarios', 'fas fa-user-circle', 'usuarios', 1, NULL),
(4, 'Categorias', 'fas fa-stream', '', 1, NULL),
(5, 'Productos', 'fas fa-box-open', 'productos', 1, NULL),
(6, 'Proveedores', 'fas fa-truck', 'proveedores', 1, NULL),
(7, 'Tipos de productos', 'fas fa-box', 'categorias', 1, 4),
(8, 'Rubros', 'fas fa-tags', 'rubros', 1, 4),
(9, 'Compras', 'fas fa-shopping-cart', 'compras', 1, NULL),
(10, 'Ventas Local', 'fas fa-cash-register', 'ventas/local', 1, NULL),
(11, 'Motos', 'fas fa-motorcycle', 'motos', 1, NULL),
(12, 'Empleados', 'fas fa-user-tie', 'empleados', 1, NULL),
(13, 'Clientes', 'fas fa-user-friends', 'clientes', 1, NULL),
(14, 'Asignación Motos', 'fas fa-clipboard-check', 'asignacion_motos', 1, NULL),
(15, 'Ventas Domicilio', 'fas fa-truck', 'ventas/domicilio', 1, NULL),
(19, 'Gestión de Envases', 'fas fa-recycle', 'envases', 1, NULL),
(20, 'Envases', 'fas fa-box', 'envases/vista', 1, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pedidos`
--

CREATE TABLE `pedidos` (
  `id_pedido` bigint(20) NOT NULL,
  `codigo` varchar(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `estado_pago` varchar(20) NOT NULL,
  `estado_pedido` varchar(20) NOT NULL,
  `fecha_entrega` datetime(6) DEFAULT NULL,
  `fecha_solicitud` datetime(6) NOT NULL,
  `monto_total` decimal(12,2) NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `id_cliente` bigint(20) NOT NULL,
  `id_empleado` bigint(20) DEFAULT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `tipo_venta` varchar(20) NOT NULL DEFAULT 'DOMICILIO',
  `fecha_limite_pago` datetime(6) DEFAULT NULL,
  `latitud` double DEFAULT NULL,
  `longitud` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `pedidos`
--

INSERT INTO `pedidos` (`id_pedido`, `codigo`, `created_at`, `estado_pago`, `estado_pedido`, `fecha_entrega`, `fecha_solicitud`, `monto_total`, `observaciones`, `subtotal`, `updated_at`, `id_cliente`, `id_empleado`, `id_usuario`, `tipo_venta`, `fecha_limite_pago`, `latitud`, `longitud`) VALUES
(1, 'NV001-0034', '2026-07-21 00:49:34', 'CANCELADO', 'RECHAZADO', NULL, '2026-07-20 19:49:34.000000', 50.00, '', 50.00, '2026-07-21 09:53:19', 28, NULL, 2, 'DOMICILIO', NULL, NULL, NULL),
(2, 'NV001-0035', '2026-07-21 01:41:44', 'PAGADO', 'ENTREGADO', '2026-07-20 20:42:54.000000', '2026-07-20 20:41:44.000000', 50.00, '', 50.00, '2026-07-21 01:42:54', 29, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(3, 'NV001-0036', '2026-07-21 02:29:58', 'PAGADO', 'ENTREGADO', '2026-07-20 21:33:37.000000', '2026-07-20 21:29:58.000000', 50.00, '', 50.00, '2026-07-21 02:33:37', 30, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(7, 'NV001-0037', '2026-07-21 03:17:06', 'COMPLETADO', 'ENTREGADO', '2026-07-20 22:40:42.000000', '2026-07-20 22:17:06.000000', 0.00, '', 0.00, '2026-07-21 03:40:42', 6, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(8, 'NV001-0038', '2026-07-21 03:20:42', 'PAGADO', 'ENTREGADO', '2026-07-20 22:41:10.000000', '2026-07-20 22:20:42.000000', 50.00, '', 50.00, '2026-07-21 03:41:10', 4, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(9, 'NV001-0039', '2026-07-21 03:37:11', 'COMPLETADO', 'ENTREGADO', '2026-07-20 22:42:41.000000', '2026-07-20 22:37:11.000000', 0.00, '', 0.00, '2026-07-21 03:42:41', 4, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(10, 'NV001-0040', '2026-07-21 03:37:39', 'PAGADO', 'ENTREGADO', '2026-07-20 22:46:55.000000', '2026-07-20 22:37:39.000000', 50.00, '', 50.00, '2026-07-21 03:46:55', 11, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(11, 'NV001-0041', '2026-07-21 03:47:55', 'COMPLETADO', 'ENTREGADO', '2026-07-20 22:52:11.000000', '2026-07-20 22:47:55.000000', 0.00, '', 0.00, '2026-07-21 03:52:11', 10, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(12, 'NV001-0042', '2026-07-21 03:53:16', 'PAGADO', 'ENTREGADO', '2026-07-21 04:57:41.000000', '2026-07-20 22:53:16.000000', 50.00, '', 50.00, '2026-07-21 09:57:41', 6, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(13, 'NV001-0043', '2026-07-21 08:42:37', 'PAGADO', 'ENTREGADO', '2026-07-21 03:43:55.000000', '2026-07-21 03:42:37.000000', 150.00, '', 150.00, '2026-07-21 08:43:55', 7, 3, 2, 'DOMICILIO', NULL, NULL, NULL),
(15, 'NV001-0044', '2026-07-21 09:49:59', 'PAGADO', 'ENTREGADO', '2026-07-21 04:56:09.000000', '2026-07-21 04:49:59.000000', 40.00, '', 40.00, '2026-07-21 09:56:09', 6, 7, 2, 'DOMICILIO', NULL, NULL, NULL),
(16, 'NV001-0045', '2026-07-21 10:15:24', 'PAGADO', 'ENTREGADO', '2026-07-25 01:39:09.000000', '2026-07-21 05:15:24.000000', 150.00, '', 150.00, '2026-07-25 06:39:09', 11, 7, 2, 'DOMICILIO', NULL, NULL, NULL),
(17, 'NV001-0046', '2026-07-21 15:37:42', 'CANCELADO', 'RECHAZADO', NULL, '2026-07-21 10:37:42.000000', 150.00, '', 150.00, '2026-07-21 15:43:38', 11, NULL, 2, 'DOMICILIO', NULL, NULL, NULL),
(18, 'NV001-0047', '2026-07-25 06:44:31', 'PAGADO', 'ENTREGADO', '2026-07-25 01:45:19.000000', '2026-07-25 01:44:31.000000', 60.00, '', 60.00, '2026-07-25 06:45:19', 1, 7, 2, 'DOMICILIO', NULL, NULL, NULL),
(19, 'NV001-0048', '2026-07-25 06:46:10', 'CANCELADO', 'RECHAZADO', NULL, '2026-07-25 01:46:10.000000', 60.00, '', 60.00, '2026-07-25 06:47:44', 7, NULL, 2, 'DOMICILIO', NULL, NULL, NULL),
(20, 'NV001-0049', '2026-07-29 03:48:31', 'PAGADO', 'ENTREGADO', '2026-07-28 22:52:21.000000', '2026-07-28 22:48:31.000000', 60.00, '', 60.00, '2026-07-29 03:52:21', 6, 3, 2, 'DOMICILIO', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pedido_pagos`
--

CREATE TABLE `pedido_pagos` (
  `id_pago` bigint(20) NOT NULL,
  `id_pedido` bigint(20) NOT NULL,
  `id_metodo` bigint(20) NOT NULL,
  `monto` decimal(12,2) NOT NULL,
  `num_operacion` varchar(50) DEFAULT NULL,
  `vuelto` decimal(10,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `pedido_pagos`
--

INSERT INTO `pedido_pagos` (`id_pago`, `id_pedido`, `id_metodo`, `monto`, `num_operacion`, `vuelto`) VALUES
(1, 2, 2, 50.00, '636363', 0.00),
(2, 3, 2, 50.00, '4455333', 0.00),
(3, 8, 3, 50.00, '545454', 0.00),
(4, 10, 3, 50.00, '3262', 0.00),
(5, 13, 1, 50.00, '', 0.00),
(6, 13, 2, 50.00, '454544', 0.00),
(7, 13, 3, 60.00, '5454', 0.00),
(8, 15, 3, 40.00, '4545', 0.00),
(9, 12, 3, 50.00, '6846', 0.00),
(10, 16, 1, 150.00, '', 0.00),
(11, 18, 1, 30.00, '', 0.00),
(12, 18, 2, 30.00, '65656', 0.00),
(13, 20, 1, 20.00, '', 0.00),
(14, 20, 2, 20.00, '737374', 0.00),
(15, 20, 3, 30.00, '77373', 0.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `perfiles`
--

CREATE TABLE `perfiles` (
  `id_perfil` bigint(20) NOT NULL,
  `nombre_perfil` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `perfiles`
--

INSERT INTO `perfiles` (`id_perfil`, `nombre_perfil`, `descripcion`, `estado`) VALUES
(1, 'Administrador', 'Acceso total al sistema y gestión de usuarios', 1),
(2, 'Vendedor', 'Gestiona ventas y productos', 1),
(3, 'vendedor1', 'adawdw', 2),
(4, 'motorizado', '', 1),
(5, 'soldador1', 'shadghshadg', 2);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `perfil_opcion`
--

CREATE TABLE `perfil_opcion` (
  `id_perfil` bigint(20) NOT NULL,
  `id_opcion` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `perfil_opcion`
--

INSERT INTO `perfil_opcion` (`id_perfil`, `id_opcion`) VALUES
(1, 19),
(2, 4),
(2, 10),
(4, 10);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos`
--

CREATE TABLE `productos` (
  `id_producto` bigint(20) NOT NULL,
  `id_categoria` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `capacidad` decimal(5,2) DEFAULT NULL,
  `unidad_medida` varchar(20) DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `precio_compra` decimal(10,2) NOT NULL,
  `precio_venta` decimal(10,2) NOT NULL,
  `stock_llenos` decimal(10,2) NOT NULL,
  `stock_vacios` int(11) NOT NULL DEFAULT 0,
  `stock_minimo` decimal(10,2) NOT NULL,
  `requiere_envase` bit(1) NOT NULL DEFAULT b'0',
  `url_imagen` longtext DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `ganancia_producto` decimal(10,2) NOT NULL,
  `stock_reservado` decimal(10,2) NOT NULL DEFAULT 0.00,
  `envase_id` bigint(20) DEFAULT NULL,
  `es_envase` tinyint(1) DEFAULT 0 COMMENT 'Es solo un envase (sin contenido)',
  `requiere_envase_id` bigint(20) DEFAULT NULL COMMENT 'Si es gas, qué envase necesita (FK a productos donde es_envase=1)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `productos`
--

INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `capacidad`, `unidad_medida`, `descripcion`, `precio_compra`, `precio_venta`, `stock_llenos`, `stock_vacios`, `stock_minimo`, `requiere_envase`, `url_imagen`, `estado`, `created_at`, `updated_at`, `ganancia_producto`, `stock_reservado`, `envase_id`, `es_envase`, `requiere_envase_id`) VALUES
(1, 5, 'Bidón de Agua', 20.00, 'L', '', 0.00, 0.00, 9.00, 0, 10.00, b'1', NULL, 1, '2026-07-20 18:09:55', '2026-07-21 04:55:33', 10.00, 0.00, 2, 0, NULL),
(2, 1, 'Balón de Gas GLP', 10.00, 'KG', '', 0.00, 0.00, 8.00, 2, 10.00, b'1', NULL, 1, '2026-07-20 18:12:04', '2026-07-28 22:49:01', 10.00, 0.00, 1, 0, NULL),
(3, 4, 'Manguera', 50.00, 'M', '', 0.00, 0.00, 0.00, 0, 1.00, b'0', NULL, 1, '2026-07-20 18:13:46', '2026-07-20 18:13:46', 5.00, 0.00, NULL, 0, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `proveedores`
--

CREATE TABLE `proveedores` (
  `id_proveedor` bigint(20) NOT NULL,
  `ruc` varchar(11) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `telefono` varchar(9) NOT NULL,
  `correo` varchar(150) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `id_rubro` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `proveedores`
--

INSERT INTO `proveedores` (`id_proveedor`, `ruc`, `nombre`, `telefono`, `correo`, `estado`, `created_at`, `updated_at`, `id_rubro`) VALUES
(3, '12345678901', 'Sipan Gas', '963852750', 'correo34@gmail.com', 1, '2026-05-21 23:46:26', '2026-05-22 00:45:04', 2),
(4, '12345678955', 'guillermo', '963852754', '', 1, '2026-05-22 00:23:15', '2026-05-22 00:53:13', 3),
(5, '11002011540', 'vitagas', '951159753', 'vitagas@gmail.com', 1, '2026-05-23 22:34:11', '2026-05-23 22:34:16', 2),
(6, '15159696455', 'AGUACIX', '969645266', '', 1, '2026-06-14 19:09:55', '2026-06-14 19:09:55', 3);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `respuestas_incidencias`
--

CREATE TABLE `respuestas_incidencias` (
  `id_respuesta` bigint(20) NOT NULL,
  `id_incidencia` bigint(20) NOT NULL,
  `moto_reemplazo` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `leido` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `respuestas_incidencias`
--

INSERT INTO `respuestas_incidencias` (`id_respuesta`, `id_incidencia`, `moto_reemplazo`, `created_at`, `updated_at`, `leido`) VALUES
(1, 1, '5425GHJ - Honda Furius 3000', '2026-07-20 19:50:34', '2026-07-21 00:50:42', 1),
(2, 2, 'Reasignado a: LEIDY BRUNO CHAVEZ', '2026-07-20 19:51:51', '2026-07-21 03:55:20', 1),
(3, 3, '', '2026-07-20 19:53:12', '2026-07-21 00:53:16', 1),
(4, 4, '9876MX - Bajaj Pulsar NS200', '2026-07-20 22:55:33', '2026-07-21 03:55:45', 1),
(5, 5, '', '2026-07-21 00:59:08', '2026-07-21 05:59:14', 1),
(6, 6, '', '2026-07-21 04:52:02', '2026-07-21 09:52:06', 1),
(7, 7, 'Reasignado a: LEIDY BRUNO CHAVEZ', '2026-07-21 04:52:26', '2026-07-21 09:58:23', 1),
(8, 2, '', '2026-07-21 04:53:19', '2026-07-21 09:58:23', 1),
(9, 9, '5425GHJ - Honda Furius 3000', '2026-07-21 04:58:19', '2026-07-21 09:58:23', 1),
(10, 10, 'Reasignado a: LEIDY BRUNO CHAVEZ', '2026-07-21 10:40:02', '2026-07-25 06:48:21', 1),
(11, 10, '', '2026-07-21 10:43:38', '2026-07-25 06:48:21', 1),
(12, 12, '54321B - Yamaha FZ25', '2026-07-25 01:45:52', '2026-07-25 06:45:56', 1),
(13, 13, 'Reasignado a: ROY AGAPITO VEGAS', '2026-07-25 01:46:31', '2026-07-25 06:46:31', 0),
(14, 14, '', '2026-07-25 01:47:11', '2026-07-25 06:48:21', 1),
(15, 13, '', '2026-07-25 01:47:44', '2026-07-25 06:47:44', 0);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `rubros`
--

CREATE TABLE `rubros` (
  `id_rubro` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `rubros`
--

INSERT INTO `rubros` (`id_rubro`, `created_at`, `descripcion`, `estado`, `nombre`, `updated_at`) VALUES
(2, '2026-05-21 17:59:37', 'gas comercio jahaja', 1, 'gas', '2026-05-22 05:44:22'),
(3, '2026-05-21 18:17:53', 'agua', 1, 'agua', '2026-05-22 05:44:37');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `transacciones_envases`
--

CREATE TABLE `transacciones_envases` (
  `id_transaccion` bigint(20) NOT NULL,
  `id_pedido` bigint(20) DEFAULT NULL,
  `id_cliente` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `tipo_transaccion` enum('INTERCAMBIO','COMPRA_ENVASE','PRESTAMO','DEVOLUCION_PRESTAMO') NOT NULL,
  `cantidad_envases` int(11) NOT NULL,
  `estado_envase_entrada` enum('VACIO','LLENO','DAÑADO','NINGUNO') DEFAULT 'VACIO',
  `fecha_plazo_devolucion` datetime DEFAULT NULL,
  `fecha_devolucion_real` datetime DEFAULT NULL,
  `estado_prestamo` enum('ACTIVO','DEVUELTO','CONVERTIDO_COMPRA','VENCIDO','NO_APLICA') DEFAULT 'NO_APLICA',
  `notas` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `id_usuario` bigint(20) NOT NULL,
  `id_perfil` bigint(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  `correo` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `fecha_creacion` datetime DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `id_empleado` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`id_usuario`, `id_perfil`, `username`, `correo`, `password`, `fecha_creacion`, `estado`, `id_empleado`) VALUES
(1, 1, 'yogacix5', 'abelordonezzapata@gmail.com', 'admin123', '2026-05-12 13:08:53', 2, NULL),
(2, 1, 'admin_zair', 'zair9@gmail.com', '$2a$10$VIUBS8.d7GhLbO3GKmGRLO.okTqHNYKp4F0pec2GCaE6u2PD85qju', '2026-05-15 05:45:50', 2, NULL),
(4, 4, 'motorizado_abel', 'abelordonez@gmail.com', '$2a$10$qepz3mo6j0A.Y3TNT44zYO.J7A5ncAOczVD7NxbwO98M99G.tDAS2', '2026-05-15 09:05:31', 2, NULL),
(5, 4, 'motorizado24', 'roy1@gmail.com', '$2a$10$WZSV3I55UwY6AHZpz9J.xudAu/0o08VoX4NnMEFUxOz4TLL9ljeTa', '2026-06-06 20:14:48', 1, 3),
(6, 2, 'admin_yovana', 'anonimo1324@ejemplo.com', '$2a$10$LHYMpZtkudvEWJBOg1fYzOzgnUn08rIE3rXORlckY5yukYb.zfgTu', '2026-06-06 20:27:54', 1, 2),
(7, 2, 'admin_alex', 'anonimo13232324@ejemplo.com', '$2a$10$nJoaLFXMvUkoNkR6Wc5CmOqEXeX2PclRkATw.mWODnk9VeGt//owW', '2026-06-06 20:29:27', 1, 4),
(8, 1, 'admin_zairK', 'zairmasnaa@gmail.com', '$2a$10$/28V8yxSKALNWVacmGwHCOzJj9M0bLl5V2JBaHJbPQo4iWUXtmASu', '2026-06-07 00:00:05', 1, 5),
(9, 4, 'repartidor_abel', 'pedro@gmail.com', '$2a$10$MqM9bR6b4S2PW4PcuuJvzeSca0qucPRcElT7ZOMcead1DQwd.NOra', '2026-06-09 08:50:11', 1, 6),
(10, 4, 'motorizado_12', 'Leydy1234@gmail.com', '$2a$10$OeTkon5eLl4C2gCEZNwBU.j/t0gjFqV5cNPZCKGG.JDiNYOqYxqVa', '2026-06-09 09:10:25', 1, 7);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `asignacion_motos`
--
ALTER TABLE `asignacion_motos`
  ADD PRIMARY KEY (`id_asignacion`),
  ADD KEY `fk_asignacion_moto` (`id_moto`),
  ADD KEY `fk_asignacion_empleado` (`id_empleado`);

--
-- Indices de la tabla `catalogo_proveedores`
--
ALTER TABLE `catalogo_proveedores`
  ADD PRIMARY KEY (`id_catalogo`),
  ADD UNIQUE KEY `uk_catalogo_proveedores` (`id_proveedor`,`id_producto`),
  ADD KEY `fk_cat_prov_maestro_producto` (`id_producto`);

--
-- Indices de la tabla `categorias`
--
ALTER TABLE `categorias`
  ADD PRIMARY KEY (`id_categoria`);

--
-- Indices de la tabla `categoria_capacidades`
--
ALTER TABLE `categoria_capacidades`
  ADD PRIMARY KEY (`id_capacidad`),
  ADD KEY `FK7vlv8bo6qunon419w1dt5ksfo` (`id_categoria`);

--
-- Indices de la tabla `clientes`
--
ALTER TABLE `clientes`
  ADD PRIMARY KEY (`id_cliente`),
  ADD KEY `idx_tipo_cliente` (`tipo_cliente`),
  ADD KEY `idx_deudor_activo` (`es_deudor_activo`),
  ADD KEY `idx_clientes_activos` (`estado`);

--
-- Indices de la tabla `compras`
--
ALTER TABLE `compras`
  ADD PRIMARY KEY (`id_compra`),
  ADD KEY `fk_compras_proveedor` (`id_proveedor`),
  ADD KEY `fk_compras_usuario` (`id_usuario`);

--
-- Indices de la tabla `configuracion`
--
ALTER TABLE `configuracion`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `ruc` (`ruc`);

--
-- Indices de la tabla `control_envase`
--
ALTER TABLE `control_envase`
  ADD PRIMARY KEY (`id_control`),
  ADD KEY `FKjggw92u227vsm2wff9j47qy0g` (`id_cliente`),
  ADD KEY `FKaoc0130sy9qwtvs42nbljmq6i` (`id_pedido`),
  ADD KEY `FKkwrtj9nbpwgn4d2yeoihmj51n` (`id_producto`);

--
-- Indices de la tabla `correlativos`
--
ALTER TABLE `correlativos`
  ADD PRIMARY KEY (`id_correlativo`),
  ADD UNIQUE KEY `serie_tipo_idx` (`tipo`,`serie`);

--
-- Indices de la tabla `detalle_compra`
--
ALTER TABLE `detalle_compra`
  ADD PRIMARY KEY (`id_detalle_compra`),
  ADD KEY `fk_detalle_compra_compra` (`id_compra`),
  ADD KEY `fk_detalle_compra_producto` (`id_producto`),
  ADD KEY `idx_detalle_compra_compra_producto` (`id_compra`,`id_producto`);

--
-- Indices de la tabla `detalle_pedido`
--
ALTER TABLE `detalle_pedido`
  ADD PRIMARY KEY (`id_detalle`),
  ADD KEY `FKh10qteor08f4cbxhsf97qtgyk` (`id_pedido`),
  ADD KEY `FKnwadx4gon4by0uw748yo8chit` (`id_producto`);

--
-- Indices de la tabla `deuda_envases`
--
ALTER TABLE `deuda_envases`
  ADD PRIMARY KEY (`id_deuda`),
  ADD KEY `idx_cliente` (`id_cliente`),
  ADD KEY `idx_estado` (`estado`),
  ADD KEY `idx_fecha_generacion` (`fecha_generacion_deuda`),
  ADD KEY `fk_deuda_producto` (`id_producto`);

--
-- Indices de la tabla `empleados`
--
ALTER TABLE `empleados`
  ADD PRIMARY KEY (`id_empleado`),
  ADD UNIQUE KEY `uk_empleado_dni` (`dni`),
  ADD UNIQUE KEY `uk_empleado_telefono` (`telefono`);

--
-- Indices de la tabla `envases`
--
ALTER TABLE `envases`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `evidencias`
--
ALTER TABLE `evidencias`
  ADD PRIMARY KEY (`id_evidencia`),
  ADD KEY `FKfu8fjk2t6ju0g3ylddevognjr` (`id_pago`);

--
-- Indices de la tabla `gastos_operativos_caja_chica`
--
ALTER TABLE `gastos_operativos_caja_chica`
  ADD PRIMARY KEY (`id_gasto`),
  ADD KEY `idx_usuario` (`id_usuario`),
  ADD KEY `idx_fecha` (`fecha_gasto`),
  ADD KEY `idx_categoria` (`categoria`);

--
-- Indices de la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  ADD PRIMARY KEY (`id_historial`),
  ADD KEY `id_producto_idx` (`id_producto`),
  ADD KEY `id_usuario_idx` (`id_usuario`);

--
-- Indices de la tabla `incidencias`
--
ALTER TABLE `incidencias`
  ADD PRIMARY KEY (`id_incidencia`),
  ADD KEY `fk_incidencia_empleado` (`id_empleado`),
  ADD KEY `fk_incidencia_pedido` (`id_pedido`);

--
-- Indices de la tabla `inventario`
--
ALTER TABLE `inventario`
  ADD PRIMARY KEY (`id_inventario`),
  ADD KEY `id_producto_idx` (`id_producto`);

--
-- Indices de la tabla `inventario_estado`
--
ALTER TABLE `inventario_estado`
  ADD PRIMARY KEY (`id_estado`),
  ADD KEY `idx_producto` (`id_producto`),
  ADD KEY `idx_fecha` (`fecha_cuadre`),
  ADD KEY `fk_inv_estado_usuario` (`id_usuario_cuadre`);

--
-- Indices de la tabla `inventario_lotes`
--
ALTER TABLE `inventario_lotes`
  ADD PRIMARY KEY (`id_lote`),
  ADD KEY `fk_lotes_maestro_producto` (`id_producto`),
  ADD KEY `fk_lotes_maestro_proveedor` (`id_proveedor`),
  ADD KEY `FK90jstbuyftp0mdyv1yxwaaxmf` (`id_compra`),
  ADD KEY `idx_inventario_lotes_producto_proveedor` (`id_producto`,`id_proveedor`);

--
-- Indices de la tabla `metas_venta`
--
ALTER TABLE `metas_venta`
  ADD PRIMARY KEY (`id_metas`),
  ADD KEY `fk_metas_venta_empleado` (`id_empleado`);

--
-- Indices de la tabla `metodo_pago`
--
ALTER TABLE `metodo_pago`
  ADD PRIMARY KEY (`id_metodo`),
  ADD UNIQUE KEY `uk_metodo_pago_nombre` (`nombre`);

--
-- Indices de la tabla `motos`
--
ALTER TABLE `motos`
  ADD PRIMARY KEY (`id_moto`),
  ADD UNIQUE KEY `uk_moto_placa` (`placa`);

--
-- Indices de la tabla `opciones`
--
ALTER TABLE `opciones`
  ADD PRIMARY KEY (`id_opciones`),
  ADD KEY `FKa8rb8t5agnw558l5pt4ys11tn` (`id_padre`);

--
-- Indices de la tabla `pedidos`
--
ALTER TABLE `pedidos`
  ADD PRIMARY KEY (`id_pedido`),
  ADD UNIQUE KEY `UKopykg0avndf87lga45wx7l02v` (`codigo`),
  ADD KEY `FKdnomiluem4t3x66t6b9aher47` (`id_cliente`),
  ADD KEY `FKltrtqgh9kyqgjst49dj88e4ra` (`id_empleado`),
  ADD KEY `FK4a0lfwlpmytywxpwjfa1a3ar2` (`id_usuario`),
  ADD KEY `idx_pedidos_cliente` (`id_cliente`),
  ADD KEY `idx_pedidos_fecha` (`fecha_solicitud`);

--
-- Indices de la tabla `pedido_pagos`
--
ALTER TABLE `pedido_pagos`
  ADD PRIMARY KEY (`id_pago`),
  ADD KEY `fk_pago_pedido` (`id_pedido`),
  ADD KEY `fk_pago_metodo` (`id_metodo`);

--
-- Indices de la tabla `perfiles`
--
ALTER TABLE `perfiles`
  ADD PRIMARY KEY (`id_perfil`);

--
-- Indices de la tabla `perfil_opcion`
--
ALTER TABLE `perfil_opcion`
  ADD PRIMARY KEY (`id_perfil`,`id_opcion`),
  ADD KEY `fk_perfil_opcion_opcion` (`id_opcion`);

--
-- Indices de la tabla `productos`
--
ALTER TABLE `productos`
  ADD PRIMARY KEY (`id_producto`),
  ADD KEY `fk_productos_categoria` (`id_categoria`),
  ADD KEY `FK1bnxwev6g4x4g913wud18tsj1` (`envase_id`),
  ADD KEY `fk_productos_requiere_envase` (`requiere_envase_id`);

--
-- Indices de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  ADD PRIMARY KEY (`id_proveedor`),
  ADD UNIQUE KEY `uk_proveedor_ruc` (`ruc`),
  ADD KEY `FKlr1ebrvvwrq71lbdot9t4wyqy` (`id_rubro`);

--
-- Indices de la tabla `respuestas_incidencias`
--
ALTER TABLE `respuestas_incidencias`
  ADD PRIMARY KEY (`id_respuesta`),
  ADD KEY `fk_respuesta_incidencia` (`id_incidencia`);

--
-- Indices de la tabla `rubros`
--
ALTER TABLE `rubros`
  ADD PRIMARY KEY (`id_rubro`);

--
-- Indices de la tabla `transacciones_envases`
--
ALTER TABLE `transacciones_envases`
  ADD PRIMARY KEY (`id_transaccion`),
  ADD KEY `idx_cliente` (`id_cliente`),
  ADD KEY `idx_producto` (`id_producto`),
  ADD KEY `idx_tipo` (`tipo_transaccion`),
  ADD KEY `idx_estado_prestamo` (`estado_prestamo`),
  ADD KEY `idx_fecha_plazo` (`fecha_plazo_devolucion`),
  ADD KEY `fk_trx_env_pedido` (`id_pedido`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `uk_usuario_username` (`username`),
  ADD UNIQUE KEY `uk_usuario_correo` (`correo`),
  ADD UNIQUE KEY `UK63uan38l9kwu9fx50ir7s0925` (`id_empleado`),
  ADD KEY `fk_usuarios_perfil` (`id_perfil`),
  ADD KEY `idx_usuarios_activos` (`estado`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `asignacion_motos`
--
ALTER TABLE `asignacion_motos`
  MODIFY `id_asignacion` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `catalogo_proveedores`
--
ALTER TABLE `catalogo_proveedores`
  MODIFY `id_catalogo` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `id_categoria` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `categoria_capacidades`
--
ALTER TABLE `categoria_capacidades`
  MODIFY `id_capacidad` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `clientes`
--
ALTER TABLE `clientes`
  MODIFY `id_cliente` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- AUTO_INCREMENT de la tabla `compras`
--
ALTER TABLE `compras`
  MODIFY `id_compra` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `configuracion`
--
ALTER TABLE `configuracion`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `control_envase`
--
ALTER TABLE `control_envase`
  MODIFY `id_control` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `correlativos`
--
ALTER TABLE `correlativos`
  MODIFY `id_correlativo` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `detalle_compra`
--
ALTER TABLE `detalle_compra`
  MODIFY `id_detalle_compra` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `detalle_pedido`
--
ALTER TABLE `detalle_pedido`
  MODIFY `id_detalle` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT de la tabla `deuda_envases`
--
ALTER TABLE `deuda_envases`
  MODIFY `id_deuda` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `empleados`
--
ALTER TABLE `empleados`
  MODIFY `id_empleado` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT de la tabla `envases`
--
ALTER TABLE `envases`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `evidencias`
--
ALTER TABLE `evidencias`
  MODIFY `id_evidencia` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT de la tabla `gastos_operativos_caja_chica`
--
ALTER TABLE `gastos_operativos_caja_chica`
  MODIFY `id_gasto` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  MODIFY `id_historial` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `incidencias`
--
ALTER TABLE `incidencias`
  MODIFY `id_incidencia` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `inventario`
--
ALTER TABLE `inventario`
  MODIFY `id_inventario` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario_estado`
--
ALTER TABLE `inventario_estado`
  MODIFY `id_estado` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario_lotes`
--
ALTER TABLE `inventario_lotes`
  MODIFY `id_lote` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `metas_venta`
--
ALTER TABLE `metas_venta`
  MODIFY `id_metas` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `metodo_pago`
--
ALTER TABLE `metodo_pago`
  MODIFY `id_metodo` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `motos`
--
ALTER TABLE `motos`
  MODIFY `id_moto` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT de la tabla `opciones`
--
ALTER TABLE `opciones`
  MODIFY `id_opciones` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT de la tabla `pedidos`
--
ALTER TABLE `pedidos`
  MODIFY `id_pedido` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT de la tabla `pedido_pagos`
--
ALTER TABLE `pedido_pagos`
  MODIFY `id_pago` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `perfiles`
--
ALTER TABLE `perfiles`
  MODIFY `id_perfil` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `productos`
--
ALTER TABLE `productos`
  MODIFY `id_producto` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  MODIFY `id_proveedor` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `respuestas_incidencias`
--
ALTER TABLE `respuestas_incidencias`
  MODIFY `id_respuesta` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `rubros`
--
ALTER TABLE `rubros`
  MODIFY `id_rubro` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `transacciones_envases`
--
ALTER TABLE `transacciones_envases`
  MODIFY `id_transaccion` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id_usuario` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `asignacion_motos`
--
ALTER TABLE `asignacion_motos`
  ADD CONSTRAINT `FK9l0tjcbajvd7wcrape4b3rrvm` FOREIGN KEY (`id_moto`) REFERENCES `motos` (`id_moto`),
  ADD CONSTRAINT `FKnlympgjovss2eqcjih4yv0gsm` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);

--
-- Filtros para la tabla `catalogo_proveedores`
--
ALTER TABLE `catalogo_proveedores`
  ADD CONSTRAINT `fk_cat_prov_maestro_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_cat_prov_maestro_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE CASCADE;

--
-- Filtros para la tabla `categoria_capacidades`
--
ALTER TABLE `categoria_capacidades`
  ADD CONSTRAINT `FK7vlv8bo6qunon419w1dt5ksfo` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`);

--
-- Filtros para la tabla `compras`
--
ALTER TABLE `compras`
  ADD CONSTRAINT `FKkypgd762ocsq30thp7sxxhd20` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`),
  ADD CONSTRAINT `FKsfjim1druo8tc28uvbb799tc4` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `control_envase`
--
ALTER TABLE `control_envase`
  ADD CONSTRAINT `FKaoc0130sy9qwtvs42nbljmq6i` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`),
  ADD CONSTRAINT `FKjggw92u227vsm2wff9j47qy0g` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `FKkwrtj9nbpwgn4d2yeoihmj51n` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`);

--
-- Filtros para la tabla `detalle_compra`
--
ALTER TABLE `detalle_compra`
  ADD CONSTRAINT `FK24e1stplndaucn3dao9chwo8p` FOREIGN KEY (`id_compra`) REFERENCES `compras` (`id_compra`),
  ADD CONSTRAINT `FKssv9q9wdop1s864p7y43e6no1` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`);

--
-- Filtros para la tabla `detalle_pedido`
--
ALTER TABLE `detalle_pedido`
  ADD CONSTRAINT `FKh10qteor08f4cbxhsf97qtgyk` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`),
  ADD CONSTRAINT `FKnwadx4gon4by0uw748yo8chit` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`);

--
-- Filtros para la tabla `deuda_envases`
--
ALTER TABLE `deuda_envases`
  ADD CONSTRAINT `fk_deuda_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_deuda_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE;

--
-- Filtros para la tabla `evidencias`
--
ALTER TABLE `evidencias`
  ADD CONSTRAINT `FKfu8fjk2t6ju0g3ylddevognjr` FOREIGN KEY (`id_pago`) REFERENCES `pedido_pagos` (`id_pago`);

--
-- Filtros para la tabla `gastos_operativos_caja_chica`
--
ALTER TABLE `gastos_operativos_caja_chica`
  ADD CONSTRAINT `fk_gasto_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  ADD CONSTRAINT `fk_historial_productos` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_historial_usuarios` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `incidencias`
--
ALTER TABLE `incidencias`
  ADD CONSTRAINT `fk_incidencia_empleado` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_incidencia_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`) ON DELETE SET NULL;

--
-- Filtros para la tabla `inventario`
--
ALTER TABLE `inventario`
  ADD CONSTRAINT `fk_inventario_productos` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `inventario_estado`
--
ALTER TABLE `inventario_estado`
  ADD CONSTRAINT `fk_inv_estado_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_inv_estado_usuario` FOREIGN KEY (`id_usuario_cuadre`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL;

--
-- Filtros para la tabla `inventario_lotes`
--
ALTER TABLE `inventario_lotes`
  ADD CONSTRAINT `FK90jstbuyftp0mdyv1yxwaaxmf` FOREIGN KEY (`id_compra`) REFERENCES `compras` (`id_compra`),
  ADD CONSTRAINT `fk_lotes_maestro_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_lotes_maestro_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE CASCADE;

--
-- Filtros para la tabla `metas_venta`
--
ALTER TABLE `metas_venta`
  ADD CONSTRAINT `FKtj8kt96ium9a4no01gm0wisxo` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);

--
-- Filtros para la tabla `opciones`
--
ALTER TABLE `opciones`
  ADD CONSTRAINT `FKa8rb8t5agnw558l5pt4ys11tn` FOREIGN KEY (`id_padre`) REFERENCES `opciones` (`id_opciones`);

--
-- Filtros para la tabla `pedidos`
--
ALTER TABLE `pedidos`
  ADD CONSTRAINT `FK4a0lfwlpmytywxpwjfa1a3ar2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `FKdnomiluem4t3x66t6b9aher47` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `FKltrtqgh9kyqgjst49dj88e4ra` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);

--
-- Filtros para la tabla `pedido_pagos`
--
ALTER TABLE `pedido_pagos`
  ADD CONSTRAINT `fk_pago_metodo` FOREIGN KEY (`id_metodo`) REFERENCES `metodo_pago` (`id_metodo`),
  ADD CONSTRAINT `fk_pago_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`) ON DELETE CASCADE;

--
-- Filtros para la tabla `perfil_opcion`
--
ALTER TABLE `perfil_opcion`
  ADD CONSTRAINT `FKccootfr17pdgjedgifd92qao0` FOREIGN KEY (`id_opcion`) REFERENCES `opciones` (`id_opciones`),
  ADD CONSTRAINT `FKe1pcyxsiyjjqt8g486euwsxft` FOREIGN KEY (`id_perfil`) REFERENCES `perfiles` (`id_perfil`);

--
-- Filtros para la tabla `productos`
--
ALTER TABLE `productos`
  ADD CONSTRAINT `FK1bnxwev6g4x4g913wud18tsj1` FOREIGN KEY (`envase_id`) REFERENCES `envases` (`id`),
  ADD CONSTRAINT `FKdtoa37luoxhhvbicrfiu5ygbj` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`),
  ADD CONSTRAINT `fk_productos_requiere_envase` FOREIGN KEY (`requiere_envase_id`) REFERENCES `productos` (`id_producto`);

--
-- Filtros para la tabla `proveedores`
--
ALTER TABLE `proveedores`
  ADD CONSTRAINT `FKlr1ebrvvwrq71lbdot9t4wyqy` FOREIGN KEY (`id_rubro`) REFERENCES `rubros` (`id_rubro`);

--
-- Filtros para la tabla `respuestas_incidencias`
--
ALTER TABLE `respuestas_incidencias`
  ADD CONSTRAINT `fk_respuesta_incidencia` FOREIGN KEY (`id_incidencia`) REFERENCES `incidencias` (`id_incidencia`) ON DELETE CASCADE;

--
-- Filtros para la tabla `transacciones_envases`
--
ALTER TABLE `transacciones_envases`
  ADD CONSTRAINT `fk_trx_env_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_trx_env_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_trx_env_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE;

--
-- Filtros para la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD CONSTRAINT `FK19wi2qritofjhcfgi2h1qpiw7` FOREIGN KEY (`id_perfil`) REFERENCES `perfiles` (`id_perfil`),
  ADD CONSTRAINT `FKgqymju3ywshi678hefxf52ev6` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
