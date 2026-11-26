# Guía de examen práctico — Sistemas Distribuidos (SIS-258)

Todo para copiar, pegar, ejecutar y **defender**. Está pensada para alguien que nunca vio la materia.

## ¿Primera vez? Sigue este camino
1. [Preparar la computadora](00-preparacion/README.md) (con internet).
2. Leer [qué es cada cosa](01-conceptos/README.md) (10 minutos).
3. Ejecutar la [plantilla 01](plantillas/01-socket-tcp-java/README.md): tu primer cliente y servidor.
4. Hacer los [ejercicios](ejercicios/README.md) del 1 en adelante, **escribiendo tú** y explicando en voz alta.
5. Repasar [la defensa](defensa.md).

## Cómo usarla (en el examen)

> **Receta completa paso a paso:** [COMO-RESOLVER.md](COMO-RESOLVER.md) (qué hacer en los primeros 5 minutos, cómo dibujar, en qué orden construir, cómo llamar de un servicio a otro y qué código HTTP devolver).

1. Lee el enunciado y **subraya las tecnologías** (socket, RMI, SOAP, REST, GraphQL, JGroups, JWT, web, escritorio).
2. Busca en la tabla de abajo: si se parece a un **ejercicio**, abre ese. Si no, arma el tuyo juntando **plantillas**.
3. Levanta los programas **de atrás hacia adelante**: primero los que responden (bancos, servicios), al final el cliente.
4. Prueba cada pieza **sola** antes de conectarla con la siguiente.

> Antes del día del examen, con internet: [00-preparacion](00-preparacion/README.md). Sin eso, sin internet, Laravel y Node **no arrancan**.
>
> **¿La PC del examen no tiene nada instalado de composer/npm?** Usa [00-preparacion/sin-internet](00-preparacion/sin-internet/README.md): proyectos base de Laravel y Node **con todas las dependencias** (`vendor` y `node_modules` comprimidos) y el jar de JGroups. Solo se descomprimen.

## Me piden… → voy a…

| Si el enunciado dice | Plantilla | Ejercicio parecido |
|---|---|---|
| "socket", "TCP", "puerto", "protocolo" | [01 Socket TCP](plantillas/01-socket-tcp-java/README.md) | [1](ejercicios/1-sockets/README.md) |
| "UDP", "datagrama" | [02 Socket UDP](plantillas/02-socket-udp-java/README.md) | [1 v6](ejercicios/1-sockets/README.md#versión-6--udp-antifraude) |
| "RMI", "invocación remota", "interfaz remota" | [03 RMI](plantillas/03-rmi-java/README.md) | [2](ejercicios/2-rmi-sockets/README.md) |
| "grupo", "sin servidor central", "réplica", "JGroups", "subasta" | [04 JGroups](plantillas/04-jgroups-java/README.md) | [7](ejercicios/7-jgroups/README.md) |
| "SOAP", "ASMX", "WSDL", "servicio web en C#" | [05 SOAP C#](plantillas/05-soap-csharp/README.md) | [4](ejercicios/4-multas/README.md), [6](ejercicios/6-central-pagos/README.md) |
| "REST", "API", "recurso", "GET/POST", "Laravel" | [06 REST Laravel](plantillas/06-rest-laravel/README.md) | [4](ejercicios/4-multas/README.md), [5](ejercicios/5-reservas-hotel/README.md), [9](ejercicios/9-transporte/README.md), [10](ejercicios/10-clinica/README.md) |
| "token", "JWT", "login", "protegido" | [07 JWT](plantillas/07-jwt-laravel/README.md) | [3](ejercicios/3-pago-en-linea/README.md), [5](ejercicios/5-reservas-hotel/README.md), [9](ejercicios/9-transporte/README.md) (a mano), [10](ejercicios/10-clinica/README.md) (firebase), [12](ejercicios/12-elecciones/README.md) (roles, 403) |
| "GraphQL", "query", "mutation", "Node" | [08 GraphQL](plantillas/08-graphql-node/README.md) | [4](ejercicios/4-multas/README.md), [5](ejercicios/5-reservas-hotel/README.md), [10](ejercicios/10-clinica/README.md) |
| "REST en Node", "Express" | [13 REST Node](plantillas/13-rest-node/README.md) | [8](ejercicios/8-cotizaciones/README.md), [11](ejercicios/11-inscripciones/README.md) |
| "roles", "solo el admin", "403" | [07 JWT](plantillas/07-jwt-laravel/README.md) | [12](ejercicios/12-elecciones/README.md) |
| "reporte", "resultados", "porcentaje" desde otro servicio | [08 GraphQL](plantillas/08-graphql-node/README.md) | [12](ejercicios/12-elecciones/README.md) |
| "GraphQL en Laravel" | [14 GraphQL Laravel](plantillas/14-graphql-laravel/README.md) | [8](ejercicios/8-cotizaciones/README.md), [11](ejercicios/11-inscripciones/README.md) |
| "SOAP en Java" | [15 SOAP Java](plantillas/15-soap-java/README.md) | [8](ejercicios/8-cotizaciones/README.md) |
| "un servicio llama a otro" (GraphQL → REST con token) | [08](plantillas/08-graphql-node/README.md) + [07](plantillas/07-jwt-laravel/README.md) | [6](ejercicios/6-central-pagos/README.md), [10](ejercicios/10-clinica/README.md), [11](ejercicios/11-inscripciones/README.md) |
| "REST en Java" | [09 REST Java](plantillas/09-rest-java/README.md) | [3](ejercicios/3-pago-en-linea/README.md) |
| "cliente web", "HTML", "JavaScript" | [10 Cliente web](plantillas/10-cliente-web/README.md) | [4](ejercicios/4-multas/README.md), [11](ejercicios/11-inscripciones/README.md), [12](ejercicios/12-elecciones/README.md) |
| "cliente de escritorio", "Windows Forms", "C#" | [11 Forms C#](plantillas/11-cliente-forms-csharp/README.md) · [16 proyectos VS listos](plantillas/16-proyectos-visual-studio/README.md) | [4](ejercicios/4-multas/README.md), [6](ejercicios/6-central-pagos/README.md), [9](ejercicios/9-transporte/README.md), [10](ejercicios/10-clinica/README.md) |
| "cliente de escritorio en Java" | [12 Cliente Java](plantillas/12-cliente-java/README.md) | — |

## Ejercicios y sus versiones

| # | Ejercicio | Versiones | Tecnologías |
|---|---|---|---|
| 1 | [Sockets](ejercicios/1-sockets/README.md) | validar documento · operaciones · banco · conversación · chat · UDP · servidor web | TCP, UDP |
| 2 | [RMI + sockets](ejercicios/2-rmi-sockets/README.md) | consultas judiciales · turismo · cambiar de tema | RMI, TCP, UDP |
| 3 | [Pago en línea](ejercicios/3-pago-en-linea/README.md) | pagar · consultar saldo · historial | RMI, TCP, REST Java, REST Laravel, JWT |
| 4 | [Multas de tránsito](ejercicios/4-multas/README.md) | completa · anular · total adeudado · otro tema | SOAP, REST, GraphQL, TCP, web, Forms |
| 5 | [Reservas hoteleras](ejercicios/5-reservas-hotel/README.md) | reservar · cancelar · comparar | REST+JWT, GraphQL, SOAP, web, Forms |
| 6 | [Central de pagos](ejercicios/6-central-pagos/README.md) | **contrato oficial (v0)** · consultar y pagar · pagar todo · más empresas · pasajes | SOAP que consume REST y GraphQL |
| 7 | [JGroups](ejercicios/7-jgroups/README.md) | chat replicado · subasta · invocación en grupo · **control de aforo (1er parcial)** | JGroups |
| 8 | [Cotizaciones BCB (Práctica 5)](ejercicios/8-cotizaciones/README.md) | GraphQL Laravel · REST Node · SOAP Java | tecnologías "al revés" |
| 9 | [Terminal de buses (2º parcial del docente)](ejercicios/9-transporte/README.md) | salidas · venta de asientos · JWT a mano | REST Laravel, JWT, Forms |
| 10 | [Clínica: citas](ejercicios/10-clinica/README.md) | pacientes REST · citas GraphQL que llama al REST · C# a los dos | REST+JWT, GraphQL, Forms |
| 11 | [Inscripción de materias](ejercicios/11-inscripciones/README.md) | GraphQL Laravel que llama a REST Node · cupo, deudas, máximo de materias | GraphQL Laravel, REST Node, web |
| 12 | [Cómputo electoral](ejercicios/12-elecciones/README.md) | `elecicion-api` del docente con roles · actas · resultados y segunda vuelta | REST+JWT+roles, GraphQL Node (misma BD), web |

## Otras secciones

| Carpeta / archivo | Para qué |
|---|---|
| [00-preparacion](00-preparacion/README.md) | Instalar y preparar **con** internet; trabajar **sin** internet |
| [01-conceptos](01-conceptos/README.md) | Qué es cada cosa, en palabras simples |
| [logica](logica/README.md) | Pedazos de lógica para agregar encima de las plantillas |
| [defensa.md](defensa.md) | Preguntas probables y cómo contestarlas |
| [errores.md](errores.md) | "Me sale este error" → solución |
| [COMO-RESOLVER.md](COMO-RESOLVER.md) | Receta paso a paso para cualquier enunciado |
| [INFORME-REVISION.md](INFORME-REVISION.md) | Qué se revisó, corrigió y agregó |
| [COMO-COLABORAR.md](COMO-COLABORAR.md) | Reglas para aumentar o corregir, y cómo debe leer una IA este repositorio |
| [repodistri.md](repodistri.md) | Resumen ("skill") del repositorio del curso: cómo escribe y cómo pregunta el docente |

## Puertos que usa toda la guía

| Programa | Puerto |
|---|---|
| Socket TCP | 5002 |
| Socket UDP | 6789 |
| RMI | 1099 (segundo registro: 1100) |
| Laravel | 8000 (otros: 8001, 8002) |
| GraphQL (Node) | 4000 |
| REST en Node | 3000 |
| SOAP en Java | 8086 |
| Login ATC / REST en Java | 8083 / 8085 |
| MySQL (XAMPP) | 3306 |
| SOAP (Visual Studio) | lo elige Visual Studio: míralo en la barra del navegador |

## De dónde sale cada cosa

| Marca | Significa |
|---|---|
| **[INGE]** | Está en la hoja de comandos o en el repositorio del curso |
| **[PRÁCTICA]** | Sale de las prácticas de la materia |
| **[EXTRA]** | Agregado de esta guía. Si en tu examen solo permiten lo del docente, **no lo uses** |

## Qué está probado y qué no

- **Ejecutado y probado** (en Linux, con Java 21, PHP 8.3, Laravel 12, Node 22 y MariaDB): sockets, RMI, JGroups, REST Laravel, JWT (con y sin librería), GraphQL, REST Java, cliente web y los flujos de los ejercicios 1 a 7.
- **Siguiendo la guía desde cero**, copiando solo de los `.md`: ejercicios 1 al 7 (con las versiones de 2, 3, 5 y 6; todas las de 1 y 7) y plantillas 01 a 04, 06 a 09. Los comandos que descargan de internet (`composer`, `laravel new`) no se probaron en esa pasada: se partió de un proyecto base ya preparado.
- **SOAP C#**: probado con Mono y un servidor de prueba, **no** con Visual Studio / IIS Express.
- **Windows Forms C#**: solo se comprobó que **compila**; no se abrió la ventana.
- **Agregado en la revisión** (ver [INFORME-REVISION.md](INFORME-REVISION.md)): plantillas 13, 14, 15 y ejercicios 8 a 12, v0 del 6 y v4 del 7 **se ejecutaron** (las páginas web de 11 y 12 con un navegador real) (Laravel con SQLite, Node 22, Java 21; el SOAP Java se probó con un cliente SOAP real). Las bases de `sin-internet` se armaron y probaron. Los proyectos de Visual Studio (plantilla 16 y `proyecto-vs` de 9 y 10) **no se compilaron** (no hay compilador de C# en Linux).
- **No probado**: los menús de Visual Studio, NetBeans y XAMPP en Windows, los `.bat`, dos computadoras en red y Laravel 13. Ensáyalo en la PC donde vas a rendir **antes** del examen.
