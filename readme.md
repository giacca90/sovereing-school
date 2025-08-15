```
 _________________________________________________________________________________
|                                                                                 |
|  GGGGGG    IIII     AAA      CCCCCC    CCCCCC      AAA      9999999     00000   |
| GG    GG    II     AA AA    CC    CC  CC    CC    AA AA    99     99   00   00  |
| GG          II    AA   AA   CC        CC         AA   AA   99     99  00     00 |
| GG   GGGG   II   AA     AA  CC        CC        AA     AA   99999999  00     00 |
| GG    GG    II   AAAAAAAAA  CC        CC        AAAAAAAAA         99  00     00 |
| GG    GG    II   AA     AA  CC    CC  CC    CC  AA     AA  99     99   00   00  |
|  GGGGGG    IIII  AA     AA   CCCCCC    CCCCCC   AA     AA   9999999     00000   |
|_________________________________________________________________________________|
```

# Plataforma de Cursos Soberanos (Sovereign School)

## Una web completa de corsos OnDemand, con chat, streaming de video, emisiones en directo y mucho mas!!!

### Características:

#### Front-end

> Hecho con Angular 20.

> Con Server Side Rendering.

> Utiliza TailwindCSS 4.

> Reproducción de videos adaptativo con Video.js.

#### Back-end

> Hecho con SpringBoot 3.5

> Arquitectura de micro-servicios.

> Utiliza base de datos PostgreSQL y MongoDB.

> Utiliza Hibernate con JPA y consultas JPQL.

> Conversión y servicio de videos HLS con FFMPEG.

> Utiliza JSON Web Token y Spring Security

### Como probarla:

La mejor forma es con docker-compose, ya está todo configurado, menos, claro está, las variables de entorno.

hay que crear los archivos ".env.back_base", ".env.back_chat", ".env.back_stream" en la carpeta "sov_school_back", el ".env" en la carpeta "front", y entonces se puede correr docker-compose.

Además, para poder ver algo, hace falta popular la base de datos, porque por defecto está totalmente vacía, tampoco hay registrado un administrador que permita crear cursos o promuover a otro usuario.
