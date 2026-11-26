# 16 · Proyectos de Visual Studio listos para abrir

[← Plantillas](../README.md) · [Índice](../../README.md) · **[EXTRA]**

Para no perder tiempo con *Nuevo proyecto* en el examen: doble clic en el `.sln` y listo. Son **.NET Framework 4.7.2** (lo que usa el docente) y **no necesitan NuGet ni internet**.

| Carpeta | Qué es | Abre | Puerto |
|---|---|---|---|
| `SitioServicio/` | Sitio web ASMX con el `Servicio.asmx` de la [plantilla 05](../05-soap-csharp/README.md) (`Verificar`, `Buscar`, `Listar`) | `SitioServicio.sln` → `Ctrl + F5` | **51305** (fijo en el `.sln`, igual que `SitioMatematicas` del docente) |
| `ClienteForms/` | Windows Forms con el `Form1.cs` de la [plantilla 11](../11-cliente-forms-csharp/README.md) (REST, GraphQL, SOAP) y la referencia `System.Web.Extensions` ya agregada | `ClienteForms.sln` → `F5` | — |

## SitioServicio paso a paso
1. Copia la carpeta `SitioServicio` a `C:\examen\` (rutas cortas) y abre `SitioServicio.sln`.
2. En el Explorador de soluciones, clic en `Servicio.asmx` → `Ctrl + F5`.
3. Se abre `http://localhost:51305/Servicio.asmx`. El WSDL: `http://localhost:51305/Servicio.asmx?WSDL`.
4. Tu lógica va en `App_Code/Servicio.cs`. Si renombras la clase, cambia también `Class="..."` en `Servicio.asmx`.

Sin Visual Studio abierto también se puede levantar con IIS Express:
```bat
"C:\Program Files\IIS Express\iisexpress.exe" /path:"C:\examen\SitioServicio\SitioServicio" /port:51305
```

## ClienteForms paso a paso
1. Copia `ClienteForms` a `C:\examen\` y abre `ClienteForms.sln`.
2. (Solo si usas SOAP) con el servicio encendido: clic derecho en el proyecto → *Agregar → Referencia de servicio* → `http://localhost:51305/Servicio.asmx?WSDL` → *Ir* → espacio de nombres `srServicio` → Aceptar. Luego en `LlamarSoap()` quita las `//`.
3. Cambia las URL de arriba de `Form1.cs` (`REST`, `LOGIN`, `GRAPHQL`) y `F5`.

> El `Web.config` del sitio **no** usa el compilador Roslyn de NuGet (que trae la plantilla de Visual Studio): compila con el de .NET Framework. Por eso en `App_Code` escribe C# simple: nada de `$"..."` ni `=>` en propiedades.
