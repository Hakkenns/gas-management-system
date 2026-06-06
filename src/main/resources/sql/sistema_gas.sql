-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3307
-- Tiempo de generación: 06-06-2026 a las 02:05:12
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
-- Estructura de tabla para la tabla `categorias`
--

CREATE TABLE `categorias` (
  `id_categoria` bigint(20) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `categorias`
--

INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`, `created_at`, `updated_at`) VALUES
(1, 'Gas Doméstico', 'balones de gas GLP para uso doméstico en hogares, cocinas y pequeños negocios.', 1, '2026-05-15 06:25:19', '2026-05-21 13:31:30'),
(2, 'Accesorios de Gas', 'productos complementarios para la instalación y seguridad del sistema de gas, como reguladores, mangueras y abrazaderas.', 1, '2026-05-15 06:26:14', '2026-05-21 13:35:51'),
(3, 'Bidones de Agua', 'bidones de agua para consumo doméstico y comercial, en diferentes capacidades y presentaciones', 1, '2026-05-15 09:23:07', '2026-05-21 13:18:30');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `clientes`
--

CREATE TABLE `clientes` (
  `id_cliente` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `dni` varchar(8) DEFAULT NULL,
  `direccion` varchar(255) NOT NULL,
  `referencia` varchar(150) DEFAULT NULL,
  `telefono` varchar(255) NOT NULL,
  `correo` varchar(255) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `clientes`
--

INSERT INTO `clientes` (`id_cliente`, `nombre`, `dni`, `direccion`, `referencia`, `telefono`, `correo`, `estado`, `created_at`, `updated_at`) VALUES
(1, 'Esnaydeher', '73838677', 'ADSASD', 'ASDASDA', '963335241', NULL, 1, '2026-06-05 18:47:07', '2026-06-05 18:47:07');

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
(6, 4, 2, '2026-05-23 22:03:00', 'NC001-0008', 1200.00, '2026-05-23 22:03:51', '2026-05-23 22:03:51', 1);

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
(2, 8, 'NC001', 'COMPRA_NOTA');

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
(6, 6, 3, 30, 40.00);

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
(1, 2, 45.00, 1, 3);

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
(10, 'Ventas', 'fas fa-cash-register', 'ventas', 1, NULL);

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
  `num_operacion` varchar(50) DEFAULT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `id_cliente` bigint(20) NOT NULL,
  `id_empleado` bigint(20) DEFAULT NULL,
  `id_metodo` bigint(20) DEFAULT NULL,
  `id_usuario` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `pedidos`
--

INSERT INTO `pedidos` (`id_pedido`, `codigo`, `created_at`, `estado_pago`, `estado_pedido`, `fecha_entrega`, `fecha_solicitud`, `monto_total`, `num_operacion`, `observaciones`, `subtotal`, `updated_at`, `id_cliente`, `id_empleado`, `id_metodo`, `id_usuario`) VALUES
(1, 'NV001-0001', '2026-06-05 23:47:07', 'PENDIENTE', 'PENDIENTE', NULL, '2026-06-05 18:47:07.000000', 90.00, NULL, '', 90.00, '2026-06-05 23:47:07', 1, NULL, 1, 2);

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
(4, 4);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos`
--

CREATE TABLE `productos` (
  `id_producto` bigint(20) NOT NULL,
  `id_categoria` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `precio_compra` decimal(10,2) NOT NULL,
  `precio_venta` decimal(10,2) NOT NULL,
  `stock_llenos` int(11) NOT NULL DEFAULT 0,
  `stock_vacios` int(11) NOT NULL DEFAULT 0,
  `stock_minimo` int(11) NOT NULL DEFAULT 0,
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

INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `precio_compra`, `precio_venta`, `stock_llenos`, `stock_vacios`, `stock_minimo`, `requiere_envase`, `url_imagen`, `estado`, `created_at`, `updated_at`, `ganancia_producto`) VALUES
(3, 1, 'Balón de Gas 10Kg', '', 35.00, 45.00, 139, 0, 10, b'1', '', 1, '2026-05-22 00:50:49', '2026-06-05 18:47:07', 0.00),
(4, 2, 'valvula', '', 35.00, 45.00, 30, 10, 10, b'0', '', 1, '2026-05-22 00:50:53', '2026-05-23 20:05:42', 0.00),
(5, 3, 'Agua 20L', '', 8.00, 10.00, 50, 0, 10, b'1', '', 1, '2026-05-23 00:01:49', '2026-05-23 22:37:16', 0.00);

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
(5, '11002011540', 'vitagas', '951159753', 'vitagas@gmail.com', 1, '2026-05-23 22:34:11', '2026-05-23 22:34:16', 2);

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
  `id_empleado` bigint(20) DEFAULT NULL,
  `nombre` varchar(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`id_usuario`, `id_perfil`, `username`, `correo`, `password`, `fecha_creacion`, `estado`, `id_empleado`, `nombre`) VALUES
(1, 1, 'yogacix5', 'abelordonezzapata@gmail.com', 'admin123', '2026-05-12 13:08:53', 2, NULL, 'yoga'),
(2, 1, 'admin_zair', 'zair9@gmail.com', '$2a$10$VIUBS8.d7GhLbO3GKmGRLO.okTqHNYKp4F0pec2GCaE6u2PD85qju', '2026-05-15 05:45:50', 1, NULL, 'zair'),
(3, 2, 'admin_roy', 'roy1@gmail.com', '$2a$10$BRryAWqwzMt1.NkgFP76vO4sM0NCVKfXnYbSwXQcp21N0A34UbgFu', '2026-05-15 05:50:44', 1, NULL, 'royA'),
(4, 4, 'motorizado_abel', 'abelordonez@gmail.com', '$2a$10$qepz3mo6j0A.Y3TNT44zYO.J7A5ncAOczVD7NxbwO98M99G.tDAS2', '2026-05-15 09:05:31', 1, NULL, 'abel');

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
-- Indices de la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  ADD PRIMARY KEY (`id_historial`),
  ADD KEY `id_producto_idx` (`id_producto`),
  ADD KEY `id_usuario_idx` (`id_usuario`);

--
-- Indices de la tabla `inventario`
--
ALTER TABLE `inventario`
  ADD PRIMARY KEY (`id_inventario`),
  ADD KEY `id_producto_idx` (`id_producto`);

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
  ADD KEY `FKkak0y959yhogp8xvwashvv14t` (`id_metodo`),
  ADD KEY `FK4a0lfwlpmytywxpwjfa1a3ar2` (`id_usuario`);

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
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `id_categoria` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `clientes`
--
ALTER TABLE `clientes`
  MODIFY `id_cliente` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT de la tabla `compras`
--
ALTER TABLE `compras`
  MODIFY `id_compra` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

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
  MODIFY `id_detalle_compra` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `detalle_pedido`
--
ALTER TABLE `detalle_pedido`
  MODIFY `id_detalle` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT de la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  MODIFY `id_historial` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario`
--
ALTER TABLE `inventario`
  MODIFY `id_inventario` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `opciones`
--
ALTER TABLE `opciones`
  MODIFY `id_opciones` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `pedidos`
--
ALTER TABLE `pedidos`
  MODIFY `id_pedido` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT de la tabla `perfiles`
--
ALTER TABLE `perfiles`
  MODIFY `id_perfil` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `productos`
--
ALTER TABLE `productos`
  MODIFY `id_producto` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  MODIFY `id_proveedor` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `rubros`
--
ALTER TABLE `rubros`
  MODIFY `id_rubro` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id_usuario` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

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
-- Filtros para la tabla `historial_stock`
--
ALTER TABLE `historial_stock`
  ADD CONSTRAINT `fk_historial_productos` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_historial_usuarios` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `inventario`
--
ALTER TABLE `inventario`
  ADD CONSTRAINT `fk_inventario_productos` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE ON UPDATE CASCADE;

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
  ADD CONSTRAINT `FKkak0y959yhogp8xvwashvv14t` FOREIGN KEY (`id_metodo`) REFERENCES `metodo_pago` (`id_metodo`),
  ADD CONSTRAINT `FKltrtqgh9kyqgjst49dj88e4ra` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);

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
-- Filtros para la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD CONSTRAINT `FK19wi2qritofjhcfgi2h1qpiw7` FOREIGN KEY (`id_perfil`) REFERENCES `perfiles` (`id_perfil`),
  ADD CONSTRAINT `FKgqymju3ywshi678hefxf52ev6` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
