# Cómo defender (preguntas probables)

[← Volver al índice](README.md)

## La respuesta que sirve para todo
Contesta en este orden: **1) qué hace** → **2) quién llama a quién y con qué** → **3) por qué esa tecnología** → **4) qué pasa si algo falla**.

Ejemplo: "Mi sistema cobra multas. La recaudadora consulta por GraphQL y paga por un socket TCP; el servidor TCP usa la API REST, que es la única que cambia datos. Si la API no responde, contesto `ERROR` y no se registra nada."

## Generales
| Pregunta | Respuesta corta |
|---|---|
| ¿Qué es un sistema distribuido? | Componentes en computadoras en red que se coordinan **pasando mensajes** y se ven como uno solo |
| ¿Desafíos? | Heterogeneidad, apertura, seguridad, escalabilidad, tolerancia a fallos, concurrencia, transparencia |
| ¿Dónde está la heterogeneidad en tu solución? | Lenguajes distintos (Java, PHP, C#, JavaScript) hablando por protocolos estándar |
| ¿Qué es middleware? | La capa que oculta la red y las diferencias entre máquinas |
| ¿Qué es transparencia? | Que el usuario no note que está distribuido (dónde está cada cosa, que hay varias) |
| ¿Cliente-servidor contra entre pares? | Roles distintos contra todos iguales (JGroups) |
| ¿Síncrono contra asíncrono? | Esperar la respuesta contra seguir sin esperar |
| ¿Comunicación directa contra indirecta? | Conozco al receptor contra hablo con un intermediario (grupo, cola, tema) |

## Sockets
| Pregunta | Respuesta |
|---|---|
| ¿Qué es un socket? | El extremo de una comunicación: IP + puerto |
| ¿TCP contra UDP? | Conexión, orden y entrega garantizada contra datagramas sueltos y más ligeros |
| ¿Para qué el hilo? | Para atender a varios clientes a la vez |
| ¿Qué pasa sin el `while(true)`? | El servidor atiende uno y termina |
| ¿Qué es el protocolo? | El formato de mensajes que acordamos |
| ¿Condición de carrera? | Dos hilos cambian el mismo dato y se pierde un cambio; se evita con `synchronized` |

## RMI
| Pregunta | Respuesta |
|---|---|
| ¿Qué es el stub? | El representante local del objeto remoto |
| ¿Para qué el registro? | Para encontrar el objeto por su nombre |
| ¿Por qué `Serializable`? | Para convertir el objeto en bytes y enviarlo |
| ¿Paso por valor o por referencia? | Los serializables se copian; los objetos remotos viajan como referencia |
| ¿Por qué `RemoteException`? | La red puede fallar en cualquier llamada |
| ¿Semánticas de invocación? | Quizás, al menos una vez, a lo sumo una vez. RMI ofrece **a lo sumo una vez** |

## SOAP, REST y GraphQL
| | SOAP | REST | GraphQL |
|---|---|---|---|
| Qué es | Protocolo | Estilo de arquitectura | Lenguaje de consulta |
| Formato | XML | Normalmente JSON | JSON |
| Contrato | WSDL | Opcional (OpenAPI) | Schema |
| Direcciones | Una | Una por recurso | Una |
| Acciones | Operaciones | Verbos HTTP | Query / Mutation |
| Errores | SoapFault | Códigos HTTP | Campo `errors` |

| Pregunta | Respuesta |
|---|---|
| ¿Idempotente? | Repetirlo deja el mismo resultado: GET, PUT, DELETE sí; POST no |
| ¿Qué significa sin estado? | El servidor no recuerda peticiones anteriores |
| ¿401 contra 403? | No sé quién eres contra sé quién eres pero no puedes |
| ¿Qué es CORS? | Una regla del **navegador**; no afecta a Java ni a curl |

## JWT
| Pregunta | Respuesta |
|---|---|
| ¿Partes? | Cabecera, payload y firma |
| ¿Está cifrado? | No: firmado. No poner claves dentro |
| ¿Si cambio el payload? | La firma ya no coincide → 401 |
| ¿Cómo se "cierra sesión"? | El cliente borra el token; vence solo. Para anularlo antes se necesita una lista negra |

## JGroups
| Pregunta | Respuesta |
|---|---|
| ¿Qué es una vista? | La lista actual de miembros |
| ¿Quién es el coordinador? | El miembro más antiguo |
| ¿Transferencia de estado? | El nuevo recibe los datos actuales |
| ¿UDP/multicast contra TCP/TCPPING? | Descubrimiento automático contra lista de direcciones |
| ¿Qué es la pila de protocolos? | Capas configurables: descubrimiento, detección de fallos, orden, retransmisión |

## Preguntas trampa sobre tu código
| Pregunta | Qué contestar |
|---|---|
| "¿Dónde se guardan los datos?" | Di la verdad: base de datos o **memoria** (se pierden al reiniciar) |
| "¿Es seguro?" | "Es una versión de examen: faltaría HTTPS, claves fuera del código y validar más" |
| "¿Qué pasa si se cae X?" | Muéstralo: apágalo y enseña el mensaje de error |
| "Cambia el puerto" | Servidor **y** cliente |
| "Agrega una operación" | RMI/SOAP: método nuevo. REST: ruta. GraphQL: campo. Socket: `case` |

## Antes de entregar
- [ ] Todo arranca desde cero en el orden correcto.
- [ ] Probaste el caso bueno **y** uno de error.
- [ ] Sabes decir qué hace cada archivo.
- [ ] Sabes qué línea cambiar si te piden otro puerto, otro campo u otra operación.

> Esta guía te da la base; **entiende lo que copias**. Cambia nombres y datos según el enunciado y practica explicarlo en voz alta.
