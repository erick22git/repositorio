https://github.com/erick22git/repositorio# Cómo colaborar (personas e IAs)

[← Volver al índice](README.md)

Esta guía mejora entre todos. Puedes **aumentar** y **corregir**; cada cambio queda como un *commit* que se revisa.

## Reglas
1. **No borres cosas que sirven.** Elimina solo si **estás seguro** de que está mal y lo probaste. Si dudas, no borres: agrega una nota o pregunta.
2. **El commit y el push los hace la persona colaboradora, nunca su IA.** Regla estricta.
3. **Un cambio por commit**, con un mensaje que diga qué y por qué.
4. **Prueba antes de subir.** Si no pudiste probarlo, escríbelo en el mensaje del commit.
5. No subas nombres de personas, contraseñas ni *tokens*, ni las carpetas `vendor`, `node_modules`, `out`, `bin`, `obj` o archivos `.env`.
6. Prohibido `git push --force`.

## Antes de editar: el código está dos veces
Cada código vive en su archivo (`.java`, `.php`, `.js`, `.cs`…) **y** copiado dentro del `README.md` de su carpeta (o de la carpeta de arriba). Si corriges uno, **corrige también el otro**.

Marca el origen de lo que agregues: **[INGE]**, **[PRÁCTICA]** o **[EXTRA]**.

## Subir un cambio (lo haces tú, a mano)
Primero pide que te agreguen como colaborador (envía tu usuario de GitHub) y **acepta la invitación**.

La primera vez (la dirección se copia en el botón verde *Code*):
```bash
git clone <URL-del-repositorio> guia
cd guia
git config user.name "TU_USUARIO"
git config user.email "TU_CORREO_DE_GITHUB"
```

Cada vez que cambies algo:
```bash
git pull
# editas tus archivos
git status
git diff
git add ruta/del/archivo
git commit -m "Corrige puerto en ejercicio 4: el cliente apuntaba a otro"
git pull --rebase
git push
```

- Revisa `git diff` antes: las líneas con `-` son las que **borras**.
- Agrega los archivos uno por uno; evita `git add .`
- Si `git pull --rebase` muestra un **conflicto** y no sabes resolverlo: `git rebase --abort` y pide ayuda.

## Para quien revisa
```bash
git pull
git log --oneline
git show CODIGO_DEL_COMMIT
git revert CODIGO_DEL_COMMIT
git push
```
`git show` muestra qué cambió. `git revert` (solo si el cambio está mal) lo deshace con un commit nuevo, sin perder el historial.

## Cómo debe leer una IA este repositorio
Pega esto a tu IA antes de pedirle algo:

```text
Vas a usar este repositorio como guía de estudio de Sistemas Distribuidos.

Orden de lectura:
1. README.md (índice y qué carpeta abrir según el pedido)
2. repodistri.md (estilo del curso y errores que no se deben copiar)
3. El README.md del ejercicio o plantilla que corresponda
4. Los archivos de código de esa carpeta

Cómo responder:
- Di en qué archivo está lo que explicas.
- Respeta las marcas [INGE], [PRÁCTICA] y [EXTRA]; avisa si usas algo que no es [INGE].
- Explica en simple, para alguien que no conoce la materia.
- Si algo no está en el repositorio o no lo probaste, dilo.

Reglas estrictas (no tienen excepción):
- No ejecutes git add, git commit, git push ni abras pull requests.
- No edites, muevas ni borres archivos del repositorio, ni uses la API o la web de GitHub.
- Si ves un error, muéstrame el archivo, la línea y el texto propuesto.
  Yo lo reviso, lo cambio, hago el commit y hago el push.
```

> Estas reglas son un acuerdo: GitHub **no puede saber** si un commit lo hizo una persona o una IA. Por eso cada quien responde por lo que sube, y cada commit se revisa.
**MUCHAS GRACIAS A TODOS LOS QUE COLABORARON O COLABORARAN**
pd: "A new age, Horus Lupercal is dead, killed by his father upon the bridge of the vengefull spirit in a final desesperate act ....."