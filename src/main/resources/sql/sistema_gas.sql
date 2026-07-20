-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3307
-- Tiempo de generación: 18-07-2026 a las 21:49:30
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

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `catalogo_proveedores`
--

CREATE TABLE `catalogo_proveedores` (
  `id_catalogo` bigint(20) NOT NULL,
  `id_proveedor` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `catalogo_proveedores`
--

INSERT INTO `catalogo_proveedores` (`id_catalogo`, `id_proveedor`, `id_producto`, `created_at`) VALUES
(4, 3, 24, '2026-06-11 22:44:12'),
(5, 3, 23, '2026-06-11 23:05:26'),
(6, 4, 24, '2026-06-12 18:01:09'),
(7, 3, 22, '2026-06-13 21:29:37'),
(8, 5, 21, '2026-06-13 21:38:17'),
(9, 4, 25, '2026-06-13 23:01:45'),
(11, 4, 26, '2026-06-13 23:03:23'),
(12, 6, 27, '2026-06-14 19:10:07'),
(13, 4, 27, '2026-06-14 19:18:23'),
(14, 3, 28, '2026-06-14 19:48:45'),
(15, 6, 29, '2026-06-18 18:20:46');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categoria_capacidades`
--

CREATE TABLE `categoria_capacidades` (
  `id_capacidad` bigint(20) NOT NULL,
  `id_categoria` bigint(20) NOT NULL,
  `valor_capacidad` decimal(5,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Índices para la tabla `categoria_capacidades`
--
ALTER TABLE `categoria_capacidades`
  ADD PRIMARY KEY (`id_capacidad`),
  ADD KEY `fk_capacidad_categoria` (`id_categoria`);

--
-- AUTO_INCREMENT de la tabla `categoria_capacidades`
--
ALTER TABLE `categoria_capacidades`
  MODIFY `id_capacidad` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Filtros para la tabla `categoria_capacidades`
--
ALTER TABLE `categoria_capacidades`
  ADD CONSTRAINT `fk_capacidad_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`) ON DELETE CASCADE;

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
(4, 'Accesorios(Por Metros)', '', 1, '2026-06-10 18:32:54', '2026-06-10 18:32:54', 'M', 1, 'Longitud (Metros)', 0);

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
  `longitud` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `clientes`
--

INSERT INTO `clientes` (`id_cliente`, `nombre`, `dni`, `direccion`, `referencia`, `telefono`, `correo`, `estado`, `created_at`, `updated_at`, `latitud`, `longitud`) VALUES
(1, 'ROY AGAPITO VEGAS', '73838677', 'AV. Pedro Cieza de León 675', 'Frente al colegio Virgen de la Paz, Chiclayo, Peru', '963335241', 'royagapito248@gmail.com', 1, '2026-06-05 18:47:07', '2026-07-16 19:31:37', NULL, NULL),
(3, 'AURISTELA GUZMAN LLANOS', '45821934', 'Av. Balta 1210', 'A media cuadra del Parque Principal, al costado del banco BCP', '963335548', 'Guzman@gmail.com', 1, '2026-06-09 06:07:57', '2026-07-15 22:44:39', NULL, NULL),
(4, 'JUAN ZARATE PAUCARCAJA', '44558663', 'Calle San José 815', 'Frente a la Plazuela Elías Aguirre, Chiclayo, Perú', '963335336', 'correo@gmail.com', 1, '2026-06-09 06:08:43', '2026-07-16 18:40:15', NULL, NULL),
(6, 'MARICIELO TORRES RAMIREZ', '72145896', 'Av. Luis Gonzales 650', 'Frente al Mercado Modelo, cerca a la cochera', '965888524', 'Maricielo@gmail.com', 1, '2026-06-09 08:52:04', '2026-07-15 22:45:15', NULL, NULL),
(7, 'NORMA BAZAN OJANAMA', '10284753', 'Av. Salaverry 820', 'Frente al Real Plaza, por la entrada peatonal', '938545399', 'Norma@gmail.com', 1, '2026-06-09 09:17:17', '2026-07-15 22:45:56', NULL, NULL),
(8, 'CARLOS QUISPE ALEJOS', '10203040', 'Av. Balta 123', 'Frente al parque principal', '965523645', 'carlos.mendoza@test.com', 1, '2026-07-17 15:29:39', '2026-07-17 15:29:39', NULL, NULL),
(9, 'LUIS HUARCAYA TORRES', '72145698', 'Calle San José 345', 'A media cuadra del óvalo', '955526488', 'ana.gomez@test.com', 1, '2026-07-17 15:30:17', '2026-07-17 15:30:17', NULL, NULL),
(10, 'EDUARDO SORIANO SAENZ', '09874521', 'Av. Grau 560', 'Frente al grifo', '953265444', 'luis.castro@test.com', 1, '2026-07-17 15:30:50', '2026-07-17 15:30:50', NULL, NULL),
(11, 'EDDY SANCHEZ MONTALBAN', '47521896', 'Av. Salaverry 410', 'A la espalda del mercado', '952202024', 'miguel.benites@test.com', 1, '2026-07-17 15:32:24', '2026-07-17 15:32:24', NULL, NULL);

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
(4, 3, 2, '2026-05-22 23:42:00', 'NC001-0006', 1000.00, '2026-05-22 23:42:32', '2026-05-22 23:54:39', 2),
(5, 3, 2, '2026-05-23 19:47:00', 'NC001-0007', 800.00, '2026-05-23 19:48:12', '2026-05-23 19:56:40', 2),
(6, 4, 2, '2026-05-23 22:03:00', 'NC001-0008', 1200.00, '2026-05-23 22:03:51', '2026-05-23 22:03:51', 1),
(7, 3, 8, '2026-06-07 21:28:00', 'NC001-0009', 90.00, '2026-06-07 21:29:27', '2026-06-07 21:29:27', 1),
(8, 5, 8, '2026-06-07 21:42:00', 'NC001-0010', 50.00, '2026-06-07 21:42:58', '2026-06-07 21:42:58', 1),
(9, 3, 8, '2026-06-07 22:19:00', 'NC001-0011', 2000.00, '2026-06-07 22:22:19', '2026-06-07 22:22:19', 1),
(10, 4, 8, '2026-06-07 22:25:00', 'NC001-0012', 90.00, '2026-06-07 22:26:22', '2026-06-07 23:13:20', 2),
(11, 5, 8, '2026-06-07 23:02:00', 'NC001-0013', 90.00, '2026-06-07 23:03:29', '2026-06-07 23:03:29', 1),
(12, 4, 8, '2026-06-07 23:19:00', 'NC001-0014', 100.00, '2026-06-07 23:20:18', '2026-06-07 23:21:11', 2),
(13, 4, 8, '2026-06-07 23:32:00', 'NC001-0015', 100.00, '2026-06-07 23:32:19', '2026-06-07 23:33:28', 2),
(14, 4, 8, '2026-06-07 23:32:00', 'NC001-0016', 50.00, '2026-06-07 23:33:00', '2026-06-07 23:33:00', 1),
(15, 3, 8, '2026-06-08 14:13:00', 'NC001-0017', 400.00, '2026-06-08 14:13:20', '2026-06-08 14:13:20', 1),
(16, 3, 8, '2026-06-09 09:12:00', 'NC001-0018', 400.00, '2026-06-09 09:13:13', '2026-06-09 09:13:13', 1),
(17, 3, 8, '2026-06-12 17:59:00', 'NC001-0019', 200.00, '2026-06-12 18:00:12', '2026-06-12 18:00:12', 1),
(18, 4, 8, '2026-06-12 18:02:00', 'NC001-0020', 500.00, '2026-06-12 18:02:42', '2026-06-12 18:02:42', 1),
(19, 3, 8, '2026-06-12 19:39:00', 'NC001-0021', 840.00, '2026-06-12 19:39:54', '2026-06-12 19:39:54', 1),
(20, 4, 8, '2026-06-12 19:45:00', 'NC001-0022', 150.00, '2026-06-12 19:45:28', '2026-06-12 19:45:28', 1),
(21, 3, 8, '2026-06-12 20:02:00', 'NC001-0023', 700.00, '2026-06-12 20:02:35', '2026-06-12 20:02:35', 1),
(22, 4, 8, '2026-06-12 20:19:00', 'NC001-0024', 40.00, '2026-06-12 20:19:37', '2026-06-12 20:19:37', 1),
(23, 3, 8, '2026-06-12 20:36:00', 'NC001-0025', 50.00, '2026-06-12 20:36:43', '2026-06-12 20:36:43', 1),
(24, 3, 8, '2026-06-13 21:25:00', 'NC001-0026', 10.00, '2026-06-13 21:26:17', '2026-06-13 21:26:17', 1),
(25, 3, 8, '2026-06-13 21:26:00', 'NC001-0027', 20.00, '2026-06-13 21:27:15', '2026-06-13 21:27:15', 1),
(26, 3, 8, '2026-06-13 21:29:00', 'NC001-0028', 50.00, '2026-06-13 21:30:08', '2026-06-13 21:30:08', 1),
(27, 3, 8, '2026-06-13 21:32:00', 'NC001-0029', 450.00, '2026-06-13 21:33:21', '2026-06-13 21:33:21', 1),
(28, 3, 8, '2026-06-13 21:56:00', 'NC001-0030', 500.00, '2026-06-13 21:57:07', '2026-06-13 21:57:23', 2),
(29, 3, 8, '2026-06-13 22:07:00', 'NC001-0031', 330.00, '2026-06-13 22:08:02', '2026-06-13 22:08:25', 2),
(30, 4, 8, '2026-06-13 23:14:00', 'NC001-0032', 1000.00, '2026-06-13 23:15:07', '2026-06-13 23:40:43', 2),
(31, 4, 8, '2026-06-13 23:15:00', 'NC001-0033', 1000.00, '2026-06-13 23:15:45', '2026-06-13 23:29:25', 2),
(32, 4, 8, '2026-06-13 23:29:00', 'NC001-0034', 200.00, '2026-06-13 23:29:58', '2026-06-13 23:30:10', 2),
(33, 4, 8, '2026-06-13 23:39:00', 'NC001-0035', 3000.00, '2026-06-13 23:39:54', '2026-06-13 23:39:54', 1),
(34, 6, 8, '2026-06-14 19:10:00', 'NC001-0036', 400.00, '2026-06-14 19:10:54', '2026-06-14 19:10:54', 1),
(35, 4, 8, '2026-06-14 19:21:00', 'NC001-0037', 300.00, '2026-06-14 19:21:43', '2026-06-14 19:21:43', 1),
(36, 4, 8, '2026-06-14 19:25:00', 'NC001-0038', 1000.00, '2026-06-14 19:26:54', '2026-06-14 19:39:29', 2),
(37, 3, 8, '2026-06-14 20:03:00', 'NC001-0039', 50.00, '2026-06-14 20:07:53', '2026-06-14 20:07:53', 1),
(38, 3, 8, '2026-06-14 20:09:00', 'NC001-0040', 100.00, '2026-06-14 20:09:13', '2026-06-14 20:09:13', 1),
(39, 4, 8, '2026-06-18 00:11:00', 'NC001-0041', 240.00, '2026-06-18 00:11:37', '2026-06-18 00:11:37', 1),
(40, 3, 8, '2026-06-18 16:36:00', 'NC001-0042', 100.00, '2026-06-18 16:37:13', '2026-06-18 16:37:13', 1),
(41, 6, 8, '2026-06-18 18:20:00', 'NC001-0043', 110.00, '2026-06-18 18:21:14', '2026-06-18 18:21:14', 1);

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
  `id_producto` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
(1, 1, 'NV001', 'VENTA_NOTA'),
(2, 43, 'NC001', 'COMPRA_NOTA');

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
(5, 5, 3, 20, 40.00),
(6, 6, 3, 30, 40.00),
(7, 7, 3, 2, 45.00),
(8, 8, 14, 1, 50.00),
(9, 9, 15, 25, 80.00),
(10, 10, 16, 2, 45.00),
(11, 11, 17, 100, 0.90),
(14, 14, 18, 50, 1.00),
(15, 15, 19, 10, 40.00),
(16, 16, 21, 101, 3.96),
(17, 17, 24, 10, 20.00),
(18, 18, 24, 20, 25.00),
(19, 19, 24, 30, 28.00),
(20, 20, 24, 5, 30.00),
(21, 21, 24, 20, 35.00),
(22, 22, 24, 1, 40.00),
(23, 23, 24, 1, 50.00),
(24, 24, 24, 1, 10.00),
(25, 25, 24, 1, 20.00),
(26, 26, 22, 1, 50.00),
(27, 27, 22, 9, 50.00),
(33, 33, 25, 10, 300.00),
(34, 34, 27, 10, 40.00),
(35, 35, 27, 15, 20.00),
(37, 37, 28, 60, 0.83),
(38, 38, 28, 120, 0.83),
(39, 39, 26, 100, 2.40),
(40, 40, 28, 60, 1.67),
(41, 41, 29, 2, 55.00);

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
  `tipo_incidencia` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
(1, 24, 3, 10.00, 10.00, 20.00, 30.00, '2026-06-12 18:00:12', '2026-06-12 18:00:12', NULL, NULL),
(2, 24, 4, 20.00, 20.00, 25.00, 30.00, '2026-06-12 18:02:42', '2026-06-12 18:04:57', NULL, NULL),
(3, 24, 3, 30.00, 30.00, 28.00, 28.00, '2026-06-12 19:39:54', '2026-06-12 19:39:54', NULL, NULL),
(4, 24, 4, 5.00, 5.00, 30.00, 30.00, '2026-06-12 19:45:28', '2026-06-12 19:45:28', NULL, NULL),
(5, 24, 3, 20.00, 20.00, 35.00, 40.00, '2026-06-12 20:02:35', '2026-06-12 20:02:35', NULL, NULL),
(6, 24, 4, 1.00, 1.00, 40.00, 40.00, '2026-06-12 20:19:37', '2026-06-12 20:19:37', NULL, NULL),
(7, 24, 3, 1.00, 1.00, 50.00, 50.00, '2026-06-12 20:36:43', '2026-06-12 20:36:43', NULL, NULL),
(8, 24, 3, 1.00, 1.00, 10.00, 20.00, '2026-06-13 21:26:17', '2026-06-13 21:26:17', NULL, NULL),
(9, 24, 3, 1.00, 1.00, 20.00, 25.00, '2026-06-13 21:27:15', '2026-06-13 21:27:15', NULL, NULL),
(10, 22, 3, 1.00, 1.00, 50.00, 70.00, '2026-06-13 21:30:08', '2026-06-13 21:30:08', NULL, NULL),
(11, 22, 3, 9.00, 9.00, 50.00, 70.00, '2026-06-13 21:33:21', '2026-06-13 21:33:21', NULL, NULL),
(12, 23, 3, 10.00, 0.00, 50.00, 80.00, '2026-06-13 21:57:07', '2026-06-13 21:57:23', 28, NULL),
(17, 25, 4, 10.00, 0.00, 300.00, 320.00, '2026-06-13 23:39:54', '2026-06-14 18:58:39', 33, NULL),
(18, 27, 6, 10.00, 0.00, 40.00, 48.00, '2026-06-14 19:10:54', '2026-07-16 22:36:17', 34, NULL),
(19, 27, 4, 15.00, 0.00, 20.00, 25.00, '2026-06-14 19:21:43', '2026-07-17 15:24:29', 35, NULL),
(21, 28, 3, 60.00, 0.00, 0.83, 4.00, '2026-06-14 20:07:53', '2026-06-17 21:50:55', 37, NULL),
(22, 28, 3, 120.00, 57.00, 0.83, 2.83, '2026-06-14 20:09:13', '2026-07-17 15:57:11', 38, NULL),
(23, 26, 4, 100.00, 29.00, 2.40, 5.40, '2026-06-18 00:11:37', '2026-07-18 14:25:54', 39, 50.00),
(24, 28, 3, 60.00, 60.00, 1.67, 3.67, '2026-06-18 16:37:13', '2026-06-18 16:37:13', 40, 60.00),
(25, 29, 6, 2.00, 0.00, 55.00, 60.00, '2026-06-18 18:21:14', '2026-07-14 16:18:59', 41, NULL);

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
(15, 'Ventas Domicilio', 'fas fa-truck', 'ventas/domicilio', 1, NULL);

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
  `ganancia_producto` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `productos`
--

INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `capacidad`, `unidad_medida`, `descripcion`, `precio_compra`, `precio_venta`, `stock_llenos`, `stock_vacios`, `stock_minimo`, `requiere_envase`, `url_imagen`, `estado`, `created_at`, `updated_at`, `ganancia_producto`) VALUES
(3, 1, 'Balón de Gas 10Kg', 10.00, 'KG', '', 35.00, 45.00, 140.00, 0, 10.00, b'1', '', 2, '2026-05-22 00:50:49', '2026-06-13 23:19:11', 0.00),
(4, 2, 'valvula', NULL, 'NO APLICA', '', 35.00, 45.00, 30.00, 10, 10.00, b'0', '', 2, '2026-05-22 00:50:53', '2026-06-13 23:19:08', 0.00),
(5, 3, 'Agua 20L', 20.00, 'L', '', 8.00, 10.00, 50.00, 0, 10.00, b'1', '', 2, '2026-05-23 00:01:49', '2026-06-13 23:19:05', 0.00),
(6, 1, 'Balón ', 10.00, 'KG', '', 0.00, 0.00, 0.00, 0, 0.00, b'0', NULL, 2, '2026-06-07 17:25:40', '2026-06-09 06:10:24', 0.00),
(7, 1, 'Balón ', 10.00, 'KG', '', 0.00, 0.00, 0.00, 0, 0.00, b'0', NULL, 2, '2026-06-07 17:25:40', '2026-06-09 06:10:21', 0.00),
(8, 3, 'agua', 20.00, 'L', '', 0.00, 0.00, 0.00, 0, 10.00, b'1', NULL, 2, '2026-06-07 17:26:37', '2026-06-09 06:10:25', 20.00),
(9, 2, 'Manguera', 50.00, 'M', '', 0.00, 0.00, 0.00, 0, 10.00, b'0', NULL, 2, '2026-06-07 18:42:08', '2026-06-09 06:10:27', 10.00),
(10, 2, 'Manguerra', 50.00, 'M', '', 0.00, 0.00, 0.00, 0, 20.00, b'0', NULL, 2, '2026-06-07 19:08:42', '2026-06-09 06:10:18', 0.00),
(11, 2, 'valculaa', NULL, 'NO_APLICA', '', 0.00, 0.00, 0.00, 0, 5.00, b'0', NULL, 2, '2026-06-07 19:22:10', '2026-06-07 19:34:58', 5.00),
(12, 2, 'valualar', NULL, 'NO APLICA', '', 0.00, 0.00, 0.00, 0, 10.00, b'0', NULL, 2, '2026-06-07 19:35:15', '2026-06-09 06:10:16', 5.00),
(13, 2, 'mageee', 60.00, 'M', '', 0.00, 0.00, 0.00, 0, 10.00, b'0', NULL, 2, '2026-06-07 19:37:14', '2026-06-09 06:10:30', 10.00),
(14, 2, 'mnnnn', 75.00, 'M', 'hihji', 0.00, 0.00, 1.00, 0, 25.00, b'0', NULL, 2, '2026-06-07 20:13:21', '2026-06-09 06:10:32', 2.00),
(15, 1, 'gaspremiun', 20.00, 'KG', '', 80.00, 85.00, 25.00, 0, 10.00, b'1', NULL, 2, '2026-06-07 22:19:48', '2026-06-13 23:19:03', 5.00),
(16, 2, 'MANGUERA', 50.00, 'M', '', 45.00, 47.00, 2.00, 0, 15.00, b'0', NULL, 2, '2026-06-07 22:25:29', '2026-06-09 06:10:48', 2.00),
(17, 2, 'MANGUERA PREMIUN', 50.00, 'M', '', 0.90, 2.90, 99.00, 0, 15.00, b'0', NULL, 2, '2026-06-07 23:02:22', '2026-06-13 23:19:02', 2.00),
(18, 2, 'MANUERAAPREMIUN', 50.00, 'M', '', 1.00, 3.00, 48.00, 0, 15.00, b'0', NULL, 2, '2026-06-07 23:19:46', '2026-06-09 06:11:11', 2.00),
(19, 1, 'gspre', 10.00, 'KG', '', 40.00, 50.00, 10.00, 0, 10.00, b'1', NULL, 2, '2026-06-08 14:11:25', '2026-06-09 06:11:14', 10.00),
(20, 1, 'Balón de Gas ', 5.00, 'KG', '', 0.00, 0.00, 0.00, 0, 0.00, b'0', NULL, 2, '2026-06-09 07:10:25', '2026-06-09 07:10:28', 0.01),
(21, 2, 'Balón de Gas 10', 500.00, 'M', '', 3.96, 5.36, 101.00, 0, 0.00, b'0', NULL, 2, '2026-06-09 07:18:08', '2026-06-13 23:19:00', 1.40),
(22, 1, 'BAONGAS', 10.00, 'KG', '', 0.00, 0.00, 0.00, 0, 0.00, b'1', NULL, 2, '2026-06-10 19:24:19', '2026-06-13 23:18:57', 20.00),
(23, 1, 'GASS', 10.00, 'KG', '', 0.00, 0.00, 0.00, 0, 0.00, b'0', NULL, 2, '2026-06-10 20:23:43', '2026-06-13 23:18:55', 30.00),
(24, 3, 'AGUAAASS', 20.00, 'L', '', 50.00, 55.00, 87.00, 0, 20.00, b'1', NULL, 2, '2026-06-10 20:24:44', '2026-06-13 23:18:52', 5.00),
(25, 2, 'VALL', NULL, 'UND', 'FINAL', 0.00, 0.00, 5.00, 0, 10.00, b'0', NULL, 1, '2026-06-13 22:53:26', '2026-06-14 18:58:45', 4.00),
(26, 4, 'MANGUERA2005', 50.00, 'M', '', 0.00, 0.00, 29.00, 0, 20.00, b'0', NULL, 1, '2026-06-13 23:02:46', '2026-07-18 14:25:54', 3.00),
(27, 3, 'aguav2', 20.00, 'L', '', 0.00, 0.00, 0.00, 0, 10.00, b'1', NULL, 1, '2026-06-14 19:07:59', '2026-07-17 15:24:29', 5.00),
(28, 4, 'MAGUERA', 60.00, 'M', '', 0.00, 0.00, 117.00, 0, 10.00, b'0', NULL, 1, '2026-06-14 19:48:01', '2026-07-17 15:57:11', 2.00),
(29, 3, 'AGUAPREMIUNV2', 30.00, 'L', '', 0.00, 0.00, 0.00, 0, 10.00, b'1', NULL, 1, '2026-06-18 18:20:32', '2026-07-14 16:18:59', 5.00);

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
-- Indices de la tabla `clientes`
--
ALTER TABLE `clientes`
  ADD PRIMARY KEY (`id_cliente`);

--
-- Indices de la tabla `compras`
--
ALTER TABLE `compras`
  ADD PRIMARY KEY (`id_compra`),
  ADD KEY `fk_compras_proveedor` (`id_proveedor`),
  ADD KEY `fk_compras_usuario` (`id_usuario`);

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
-- Indices de la tabla `empleados`
--
ALTER TABLE `empleados`
  ADD PRIMARY KEY (`id_empleado`),
  ADD UNIQUE KEY `uk_empleado_dni` (`dni`),
  ADD UNIQUE KEY `uk_empleado_telefono` (`telefono`);

--
-- Indices de la tabla `evidencias`
--
ALTER TABLE `evidencias`
  ADD PRIMARY KEY (`id_evidencia`),
  ADD KEY `FKfu8fjk2t6ju0g3ylddevognjr` (`id_pago`);

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
-- Indices de la tabla `inventario_lotes`
--
ALTER TABLE `inventario_lotes`
  ADD PRIMARY KEY (`id_lote`),
  ADD KEY `fk_lotes_maestro_producto` (`id_producto`),
  ADD KEY `fk_lotes_maestro_proveedor` (`id_proveedor`),
  ADD KEY `FK90jstbuyftp0mdyv1yxwaaxmf` (`id_compra`);

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
  ADD KEY `FK4a0lfwlpmytywxpwjfa1a3ar2` (`id_usuario`);

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
  ADD KEY `fk_productos_categoria` (`id_categoria`);

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
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `uk_usuario_username` (`username`),
  ADD UNIQUE KEY `uk_usuario_correo` (`correo`),
  ADD UNIQUE KEY `UK63uan38l9kwu9fx50ir7s0925` (`id_empleado`),
  ADD KEY `fk_usuarios_perfil` (`id_perfil`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `asignacion_motos`
--
ALTER TABLE `asignacion_motos`
  MODIFY `id_asignacion` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `catalogo_proveedores`
--
ALTER TABLE `catalogo_proveedores`
  MODIFY `id_catalogo` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `id_categoria` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `clientes`
--
ALTER TABLE `clientes`
  MODIFY `id_cliente` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT de la tabla `compras`
--
ALTER TABLE `compras`
  MODIFY `id_compra` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- AUTO_INCREMENT de la tabla `control_envase`
--
ALTER TABLE `control_envase`
  MODIFY `id_control` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `correlativos`
--
ALTER TABLE `correlativos`
  MODIFY `id_correlativo` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `detalle_compra`
--
ALTER TABLE `detalle_compra`
  MODIFY `id_detalle_compra` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- AUTO_INCREMENT de la tabla `detalle_pedido`
--
ALTER TABLE `detalle_pedido`
  MODIFY `id_detalle` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `empleados`
--
ALTER TABLE `empleados`
  MODIFY `id_empleado` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT de la tabla `evidencias`
--
ALTER TABLE `evidencias`
  MODIFY `id_evidencia` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  MODIFY `id_historial` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `incidencias`
--
ALTER TABLE `incidencias`
  MODIFY `id_incidencia` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario`
--
ALTER TABLE `inventario`
  MODIFY `id_inventario` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario_lotes`
--
ALTER TABLE `inventario_lotes`
  MODIFY `id_lote` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=26;

--
-- AUTO_INCREMENT de la tabla `motos`
--
ALTER TABLE `motos`
  MODIFY `id_moto` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT de la tabla `opciones`
--
ALTER TABLE `opciones`
  MODIFY `id_opciones` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `pedidos`
--
ALTER TABLE `pedidos`
  MODIFY `id_pedido` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `pedido_pagos`
--
ALTER TABLE `pedido_pagos`
  MODIFY `id_pago` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `perfiles`
--
ALTER TABLE `perfiles`
  MODIFY `id_perfil` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `productos`
--
ALTER TABLE `productos`
  MODIFY `id_producto` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- AUTO_INCREMENT de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  MODIFY `id_proveedor` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `respuestas_incidencias`
--
ALTER TABLE `respuestas_incidencias`
  MODIFY `id_respuesta` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `rubros`
--
ALTER TABLE `rubros`
  MODIFY `id_rubro` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

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
-- Filtros para la tabla `evidencias`
--
ALTER TABLE `evidencias`
  ADD CONSTRAINT `FKfu8fjk2t6ju0g3ylddevognjr` FOREIGN KEY (`id_pago`) REFERENCES `pedido_pagos` (`id_pago`);

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
  ADD CONSTRAINT `FKdtoa37luoxhhvbicrfiu5ygbj` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`);

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
-- Filtros para la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD CONSTRAINT `FK19wi2qritofjhcfgi2h1qpiw7` FOREIGN KEY (`id_perfil`) REFERENCES `perfiles` (`id_perfil`),
  ADD CONSTRAINT `FKgqymju3ywshi678hefxf52ev6` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;