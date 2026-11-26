# Ejercicios resueltos, con sus versiones

[← Volver al índice](../README.md)

Cada ejercicio trae: enunciado resumido, dibujo, orden de arranque, código, datos de prueba y, por **cada versión**, qué hace y cómo defenderlo.

| # | Ejercicio | Dificultad | Origen del enunciado |
|---|---|---|---|
| 1 | [Sockets TCP y UDP](1-sockets/README.md) | ★ | Repositorio del curso y práctica 1 |
| 2 | [RMI que usa sockets](2-rmi-sockets/README.md) | ★★ | Práctica 2 y resolución de examen del repositorio |
| 3 | [Pago en línea](3-pago-en-linea/README.md) | ★★★ | Parcial anterior |
| 4 | [Multas de tránsito](4-multas/README.md) | ★★★ | Parcial anterior |
| 5 | [Reservas hoteleras](5-reservas-hotel/README.md) | ★★★ | Parcial anterior |
| 6 | [Central de pagos](6-central-pagos/README.md) | ★★ | Enunciado de práctica para examen |
| 7 | [JGroups: chat, subasta, RPC, aforo](7-jgroups/README.md) | ★★ | Práctica 3 y 1er parcial |
| 8 | [Cotizaciones BCB](8-cotizaciones/README.md) | ★★★ | Práctica 5 |
| 9 | [Terminal de buses](9-transporte/README.md) | ★★ | 2º parcial del repositorio del docente (`transporte`) |
| 10 | [Clínica: citas](10-clinica/README.md) | ★★★ | Variante inventada con lo que pidió el docente (REST, GraphQL Node, C# a los dos) |
| 11 | [Inscripción de materias](11-inscripciones/README.md) | ★★★ | Variante inventada: GraphQL en Laravel que llama a REST en Node, cliente web |
| 12 | [Cómputo electoral](12-elecciones/README.md) | ★★★ | `elecicion-api` del repositorio del docente llevado a examen: roles, actas, resultados |

## Si tu examen no es ninguno de estos
Receta completa: [COMO-RESOLVER.md](../COMO-RESOLVER.md). En corto:
1. Dibuja cajas (programas) y flechas (quién llama a quién, con qué tecnología).
2. Por cada caja, abre su [plantilla](../plantillas/README.md).
3. Construye primero las cajas que **solo responden**; pruébalas solas.
4. Después las que llaman a otras; al final la pantalla del cliente.
