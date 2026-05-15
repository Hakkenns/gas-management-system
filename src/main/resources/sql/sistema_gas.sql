-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3307
-- Tiempo de generación: 15-05-2026 a las 07:50:45
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
) ;

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
) ;

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
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `control_envase`
--

CREATE TABLE `control_envase` (
  `id_control` bigint(20) NOT NULL,
  `id_cliente` bigint(20) NOT NULL,
  `id_producto` bigint(20) DEFAULT NULL,
  `id_pedido` bigint(20) DEFAULT NULL,
  `cantidad_prestada` int(11) NOT NULL,
  `fecha_prestamo` datetime NOT NULL,
  `fecha_devolucion` datetime DEFAULT NULL,
  `estado` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

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
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalle_pedido`
--

CREATE TABLE `detalle_pedido` (
  `id_detalle` bigint(20) NOT NULL,
  `id_pedido` bigint(20) NOT NULL,
  `id_producto` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL
) ;

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
) ;

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
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `metodo_pago`
--

CREATE TABLE `metodo_pago` (
  `id_metodo` bigint(20) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `estado` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `opciones`
--

CREATE TABLE `opciones` (
  `id_opciones` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `icono` varchar(100) NOT NULL,
  `ruta` varchar(150) NOT NULL,
  `estado` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `opciones`
--

INSERT INTO `opciones` (`id_opciones`, `nombre`, `icono`, `ruta`, `estado`) VALUES
(1, 'Dashboard', 'fas fa-tachometer-alt', 'dashboard', 1),
(2, 'Perfiles', 'fas fa-user-shield', 'perfiles', 1),
(3, 'Usuarios', 'fas fa-user-circle', 'usuarios', 1),
(4, 'Categorias', 'fas fa-stream', 'categorias', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pedido`
--

CREATE TABLE `pedido` (
  `id_pedido` bigint(20) NOT NULL,
  `codigo` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `estado_pago` varchar(255) NOT NULL,
  `estado_pedido` varchar(255) NOT NULL,
  `fecha_entrega` datetime(6) DEFAULT NULL,
  `fecha_solicitud` datetime(6) NOT NULL,
  `monto_total` decimal(12,2) NOT NULL,
  `num_operacion` varchar(50) DEFAULT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `id_cliente` bigint(20) NOT NULL,
  `id_empleado` bigint(20) DEFAULT NULL,
  `id_metodo` bigint(20) DEFAULT NULL,
  `id_usuario` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pedidos`
--

CREATE TABLE `pedidos` (
  `id_pedido` bigint(20) NOT NULL,
  `id_cliente` bigint(20) NOT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `id_empleado` bigint(20) DEFAULT NULL,
  `id_metodo` bigint(20) DEFAULT NULL,
  `fecha_solicitud` datetime NOT NULL,
  `fecha_entrega` datetime DEFAULT NULL,
  `codigo` varchar(20) NOT NULL,
  `monto_total` decimal(12,2) NOT NULL,
  `estado_pedido` enum('PENDIENTE','EN_PROCESO','ENTREGADO','ANULADO') NOT NULL DEFAULT 'PENDIENTE',
  `estado_pago` enum('PENDIENTE','PARCIAL','PAGADO','ANULADO') NOT NULL DEFAULT 'PENDIENTE',
  `num_operacion` varchar(50) DEFAULT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `subtotal` decimal(10,2) NOT NULL DEFAULT 0.00
) ;

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
(2, 'Vendedor', 'Gestiona ventas y productos', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `perfil_opcion`
--

CREATE TABLE `perfil_opcion` (
  `id_perfil` bigint(20) NOT NULL,
  `id_opcion` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos`
--

CREATE TABLE `productos` (
  `id_producto` bigint(20) NOT NULL,
  `id_categoria` bigint(20) NOT NULL,
  `id_proveedor` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `precio_compra` decimal(10,2) NOT NULL,
  `precio_venta` decimal(10,2) NOT NULL,
  `stock_llenos` int(11) NOT NULL DEFAULT 0,
  `stock_vacios` int(11) NOT NULL DEFAULT 0,
  `stock_minimo` int(11) NOT NULL DEFAULT 0,
  `requiere_envase` bit(1) NOT NULL DEFAULT b'0',
  `url_imagen` varchar(255) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

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
  `rubro` varchar(100) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

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
(1, 1, 'yogacix', 'abelordonezzapata@gmail.com', 'admin123', '2026-05-12 13:08:53', 1, NULL);

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
  ADD KEY `fk_control_envase_cliente` (`id_cliente`),
  ADD KEY `fk_control_envase_pedido` (`id_pedido`),
  ADD KEY `fk_control_envase_producto` (`id_producto`);

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
  ADD KEY `fk_detalle_pedido_pedido` (`id_pedido`),
  ADD KEY `fk_detalle_pedido_producto` (`id_producto`),
  ADD KEY `idx_detalle_pedido_pedido_producto` (`id_pedido`,`id_producto`);

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
  ADD PRIMARY KEY (`id_opciones`);

--
-- Indices de la tabla `pedido`
--
ALTER TABLE `pedido`
  ADD PRIMARY KEY (`id_pedido`),
  ADD UNIQUE KEY `UKp6h3xykm0g7lqxwbo6suwqujj` (`codigo`),
  ADD KEY `FKjo8pw8in6ju2iot14btshgxqp` (`id_empleado`),
  ADD KEY `FKr1co9hep4t3e7679bgmp19jli` (`id_metodo`),
  ADD KEY `FK1jrv9w6qwijj7gv47oulj0q92` (`id_usuario`),
  ADD KEY `FKr4ccr04glmc8v6myw3lx55mb6` (`id_cliente`);

--
-- Indices de la tabla `pedidos`
--
ALTER TABLE `pedidos`
  ADD PRIMARY KEY (`id_pedido`),
  ADD UNIQUE KEY `uk_pedido_codigo` (`codigo`),
  ADD KEY `idx_pedido_fecha_solicitud` (`fecha_solicitud`),
  ADD KEY `fk_pedido_cliente` (`id_cliente`),
  ADD KEY `fk_pedido_metodo` (`id_metodo`),
  ADD KEY `fk_pedido_empleado` (`id_empleado`),
  ADD KEY `fk_pedido_usuario` (`id_usuario`);

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
  ADD KEY `fk_productos_proveedor` (`id_proveedor`);

--
-- Indices de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  ADD PRIMARY KEY (`id_proveedor`),
  ADD UNIQUE KEY `uk_proveedor_ruc` (`ruc`);

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
-- AUTO_INCREMENT de la tabla `perfiles`
--
ALTER TABLE `perfiles`
  MODIFY `id_perfil` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `id_categoria` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `clientes`
--
ALTER TABLE `clientes`
  MODIFY `id_cliente` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `compras`
--
ALTER TABLE `compras`
  MODIFY `id_compra` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `control_envase`
--
ALTER TABLE `control_envase`
  MODIFY `id_control` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `detalle_compra`
--
ALTER TABLE `detalle_compra`
  MODIFY `id_detalle_compra` bigint(20) NOT NULL AUTO_INCREMENT;

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
-- AUTO_INCREMENT de la tabla `pedido`
--
ALTER TABLE `pedido`
  MODIFY `id_pedido` bigint(20) NOT NULL AUTO_INCREMENT;

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
  ADD CONSTRAINT `FKjggw92u227vsm2wff9j47qy0g` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `FKjs0oyayq4hpjje47qm9fec4ti` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
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
  ADD CONSTRAINT `FK7n9hdifr08joboojejveby1vr` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
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
-- Filtros para la tabla `pedido`
--
ALTER TABLE `pedido`
  ADD CONSTRAINT `FK1jrv9w6qwijj7gv47oulj0q92` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `FKjo8pw8in6ju2iot14btshgxqp` FOREIGN KEY (`id_empleado`) REFERENCES `empleados` (`id_empleado`),
  ADD CONSTRAINT `FKr1co9hep4t3e7679bgmp19jli` FOREIGN KEY (`id_metodo`) REFERENCES `metodo_pago` (`id_metodo`),
  ADD CONSTRAINT `FKr4ccr04glmc8v6myw3lx55mb6` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`);

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
  ADD CONSTRAINT `FK146wfsn2op2nvbfuxae33xbim` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`),
  ADD CONSTRAINT `FKdtoa37luoxhhvbicrfiu5ygbj` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`);

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
