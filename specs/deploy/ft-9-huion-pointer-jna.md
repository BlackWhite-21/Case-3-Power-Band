---
id: ft-9
layer: deploy
status: approved
---

# Dependencias y distribución del adaptador JNA Huion

## General description

- Incorporar las dependencias JNA mediante Maven para invocar Windows Pointer API desde Java.
- No compilar ni distribuir una DLL propia; permitir que JNA cargue su runtime desde la dependencia Maven.

## Inputs

- JDK 25+ x64.
- Dependencia Maven `jna` 5.17.0.
- Maven y `pom.xml`.

## Outputs

- Aplicación Java con JNA en su classpath Maven.
- Instrucciones de instalación/configuración del controlador y ejecución en `readme.md`.

## Test criteria

- Maven resuelve las dependencias JNA sin un toolchain C++/CMake/MSVC/MinGW.
- `mvn test` no requiere Windows, DLL del proyecto ni tableta física.
- En sistemas distintos de Windows o en una arquitectura no soportada, el adaptador informa que no está disponible sin impedir el uso de los otros instrumentos.
- El artefacto de distribución documenta requisitos de Windows 10/11, arquitectura x64, JDK/runtime y controlador de Huion; no instala ni reemplaza silenciosamente el controlador del fabricante.

## Assets

- Especificación del contrato JNA y compatibilidad: `../backend/ft-9-huion-pointer-jna.md`.
- Diagrama de clases: `../diagrams/ft-9-huion-pointer-jna.puml`.

## Implementation guidelines

- Declarar la dependencia `jna` en `pom.xml`; no agregar perfil de build nativo, script PowerShell ni fuente C++.
- JNA administra la carga de su runtime por plataforma. No empaquetar bibliotecas nativas propias ni redistribuir controladores/bibliotecas propietarias Huion/Wintab.
- Documentar el requisito de Windows/driver y la verificación de presión real en `readme.md`.

## Business workflow

1. Resolver JNA 5.17.0 desde Maven junto con la aplicación Java.
2. Detectar OS/arquitectura al inicializar el adaptador.
3. En Windows compatible, registrar el hook de mensajes y empezar la captura asíncrona.
4. En OS no compatible, informar disponibilidad sin bloquear otros instrumentos.
