# 01 · Qué es cada cosa (en simple)

[← Volver al índice](../README.md)

## La idea de todo
Un **sistema distribuido** son varios programas, a veces en varias computadoras, que se hablan por la red y parecen uno solo.

| Palabra | En simple |
|---|---|
| **Cliente** | El que pide |
| **Servidor** | El que espera y responde |
| **IP** | La dirección de la computadora. `localhost` = esta misma |
| **Puerto** | La "ventanilla" del programa dentro de la computadora |
| **Protocolo** | El acuerdo de cómo se escriben los mensajes. Ej. `pasaporte:123` |
| **Middleware** | Capa que te esconde los detalles de la red (RMI, SOAP, JGroups) |
| **Intermediario / orquestador** | Programa que recibe un pedido y para responderlo llama a otros |
| **Serializar** | Convertir un objeto en bytes o texto para mandarlo |
| **Concurrencia** | Atender a varios a la vez (hilos) |
| **synchronized** | "De uno en uno": evita que dos hilos cambien lo mismo a la vez |

## Formas de comunicar

| Forma | Qué es | Cómo la reconoces | Como en la vida |
|---|---|---|---|
| **Socket TCP** | Un tubo directo entre dos programas. Llega todo y en orden | "socket", "conexión" | Llamada telefónica |
| **Socket UDP** | Mandas un paquete sin saber si llegó | "datagrama", "sin conexión" | Mandar una postal |
| **RMI** | Llamas un método de un objeto que está en otra máquina (Java a Java) | "método remoto", "interfaz" | Control remoto |
| **SOAP** | Servicio que habla XML y tiene un contrato (WSDL) | "WSDL", "ASMX", "operación" | Formulario oficial con formato fijo |
| **REST** | Recursos con direcciones y verbos HTTP, en JSON | "API", "GET /multas" | Archivero: cada cajón tiene dirección |
| **GraphQL** | Una sola dirección; el cliente dice qué campos quiere | "query", "mutation" | Pedir a la carta |
| **JGroups** | Mensajes a un grupo, sin servidor central | "grupo", "réplica" | Grupo de chat |

## Directa contra indirecta
- **Directa** (sockets, RMI, REST, SOAP, GraphQL): el que llama sabe a quién llama y los dos deben estar encendidos.
- **Indirecta** (JGroups, colas, publicar/suscribir): hay un intermediario (el grupo, la cola); no hace falta saber quién recibe.

## REST
| Verbo | Hace | Responde bien con |
|---|---|---|
| GET | Leer | 200 |
| POST | Crear | 201 |
| PUT / PATCH | Cambiar todo / una parte | 200 |
| DELETE | Borrar | 204 |

| Código | Significa |
|---|---|
| 401 | No mandaste token o es inválido |
| 404 | No existe |
| 409 | Choca: ya existe o ya está en ese estado |
| 422 | Datos mal llenados (validación) |
| 500 | Error del programa: mirar `storage/logs/laravel.log` |
| 502 / 503 | Falló **otro** servicio del que dependes |

## Palabras de cada tecnología
| Palabra | En simple |
|---|---|
| **ServerSocket / accept()** | Abrir la ventanilla / esperar al siguiente cliente |
| **DatagramPacket** | El sobre de UDP |
| **Remote / UnicastRemoteObject** | "Esto se puede llamar desde afuera" / la clase que lo implementa |
| **Registry** | La guía telefónica de RMI: nombre → objeto |
| **WSDL** | El contrato de SOAP: qué operaciones hay (`?WSDL`) |
| **[WebMethod]** | Marca en C#: "este método se publica en el servicio" |
| **SoapFault** | El error de SOAP |
| **Migración** | Código que crea la tabla |
| **Modelo** | Clase que representa la tabla |
| **Controlador** | Lo que se ejecuta cuando llega una petición |
| **Seeder** | Datos de prueba |
| **Middleware (Laravel)** | El portero que revisa antes de dejar pasar |
| **JWT** | Pase firmado: `cabecera.datos.firma`. Si alguien lo cambia, la firma ya no coincide |
| **Schema / resolve** | El menú de GraphQL / la función que trae cada dato |
| **CORS** | Permiso del navegador para que una página llame a otro puerto |
| **Vista / coordinador (JGroups)** | La lista actual de miembros / el más antiguo |
