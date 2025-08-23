-- Creación de tablas si no existen
CREATE TABLE IF NOT EXISTS curso (
	id_curso SERIAL PRIMARY KEY,
	fecha_publicacion_curso TIMESTAMP NOT NULL,
	nombre_curso VARCHAR(255) NOT NULL,
	descriccion_corta TEXT,
	descriccion_larga TEXT,
	imagen_curso VARCHAR(255),
	precio_curso DECIMAL(10, 2)
);
CREATE TABLE IF NOT EXISTS clase (
	id_clase SERIAL PRIMARY KEY,
	nombre_clase VARCHAR(255) NOT NULL,
	descriccion_clase TEXT,
	contenido_clase TEXT,
	tipo_clase INTEGER NOT NULL,
	direccion_clase VARCHAR(255) NOT NULL,
	posicion_clase INTEGER NOT NULL,
	id_curso INTEGER REFERENCES curso(id_curso)
);
CREATE TABLE IF NOT EXISTS plan (
	id_plan SERIAL PRIMARY KEY,
	nombre_plan VARCHAR(50) NOT NULL,
	precio_plan DECIMAL(10, 2) NOT NULL
);
CREATE TABLE IF NOT EXISTS usuario (
	id_usuario SERIAL PRIMARY KEY,
	nombre_usuario VARCHAR(255) NOT NULL,
	foto_usuario TEXT [],
	presentacion VARCHAR(1500),
	roll_usuario VARCHAR(50) NOT NULL,
	plan_usuario INTEGER REFERENCES plan(id_plan),
	fecha_registro_usuario DATE NOT NULL,
	is_enabled BOOLEAN,
	account_no_expired BOOLEAN,
	account_no_locked BOOLEAN,
	credentials_no_expired BOOLEAN
);
CREATE TABLE IF NOT EXISTS login (
	id_login SERIAL PRIMARY KEY,
	id_usuario INTEGER REFERENCES usuario(id_usuario),
	correo_electronico VARCHAR(255) NOT NULL UNIQUE,
	PASSWORD VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS curso_profesor (
	id_curso INTEGER REFERENCES curso(id_curso),
	id_usuario INTEGER REFERENCES usuario(id_usuario),
	PRIMARY KEY (id_curso, id_usuario)
);
CREATE TABLE IF NOT EXISTS usuario_curso (
	id_usuario INTEGER REFERENCES usuario(id_usuario),
	id_curso INTEGER REFERENCES curso(id_curso),
	PRIMARY KEY (id_usuario, id_curso)
);
CREATE TABLE IF NOT EXISTS cursos_plan (
	id_curso INTEGER REFERENCES curso(id_curso),
	id_plan INTEGER REFERENCES plan(id_plan),
	PRIMARY KEY (id_curso, id_plan)
);
INSERT INTO curso (
		fecha_publicacion_curso,
		nombre_curso,
		descriccion_corta,
		descriccion_larga,
		imagen_curso,
		precio_curso
	)
VALUES (
		'2024-01-01 00:00:00',
		'Curso de la REML',
		'Aprende a utilizar la R.E.M.L.,
la acreditación que te reconoce como el Soberano que eres.',
		'Curso completo sobre la Republica Errante del Menda Lerenda,
descubre sus fundamentos jurídicos, sus potencialidades, como utilizarla con el D.N.I. o como única acreditación.
En este curso hay todo lo que necesitas saber!!!',
		'https://localhost:8080/usuario/fotos/Logo-REML.webp',
		50.00
	),
	(
		'2024-04-07 00:00:00',
		'Curso S.P.C.',
		'El mejor remedio al Fraude del nombre legal. Recupera el control sobre tus propiedades,
embargando al D.N.I.',
		'En este curso aprenderás como convertirte en Acreedor de parte asegurada, embargar en D.N.I. y convirtiéndote en su acreedor.
		Ademas aprenderás como transferir tus activos a un Trust (fideicomiso) para protegerlas y poderla transferir a tus descendientes sin pagar impuestos.',
		'https://localhost:8080/usuario/fotos/sello-ex-libris-1.webp',
		150.00
	),
	(
		'2024-08-07 00:00:00',
		'Curso de Libertad Financiera',
		'Aprende las técnicas de ingeniería financiera para dejar de pagar impuestos sobre tu trabajo !!',
		'Cansado de trabajar duro y ganar poco?? Estas harto de pagar impuestos sobre tu trabajo???
		Este curso es lo que buscas!!!
		Aprenderás las mejores técnicas de ingeniería financiera, como los Fideicomisos, las empresas off-shore y fundaciones.',
		'https://localhost:8080/usuario/fotos/roble_texto.svg',
		70.00
	),
	(
		'2024-08-18 00:00:00',
		'Curso de Copyright',
		'Desde la base hasta la practica. Registra tu nombre, tu casa, tu coche y todas tus obras como Marcas Registradas y obtiene el control absoluto.',
		'El derecho de Autor es el principal de los derechos, es el principio por lo cual el Vaticano se declaró como el propietario de toda la creación.
		Como ellos, nosotros también podemos utilizarlo como defensa contra los humanistas seculares.',
		'https://localhost:8080/usuario/fotos/Logo_LJS_morado.webp',
		40.00
	);
INSERT INTO clase (
		nombre_clase,
		descriccion_clase,
		contenido_clase,
		tipo_clase,
		direccion_clase,
		posicion_clase,
		id_curso
	)
VALUES (
		'Clase 1 - Presentación',
		'Una breve presentación del profesor',
		'Contenido de la clase',
		0,
		'/home/matt/Escritorio/Proyectos/SovereingSchool/Videos/01.mp4',
		1,
		1
	),
	(
		'Clase 2 - Introducción',
		'Introducción al curso',
		'Contenido de la clase',
		0,
		'/home/matt/Escritorio/Proyectos/SovereingSchool/Videos/02.mp4',
		2,
		1
	),
	(
		'Clase 3 - Que es la REML???',
		'Que es la REML y como se creó??',
		'Contenido de la clase',
		0,
		'http://curso1/clase3',
		3,
		1
	),
	(
		'Clase 4 - Fundamentos legales',
		'El convenio de Montevideo y otros',
		'Contenido de la clase',
		0,
		'http://curso1/clase4',
		4,
		1
	),
	(
		'Clase 1 - Que es un Trust??',
		'Entendemos el Fideicomiso',
		'Contenido de la clase',
		0,
		'http://curso2/clase1',
		1,
		2
	),
	(
		'Clase 2 - Partes del Trust',
		'Fideicomitente, Fideicomisario y Beneficiario',
		'Contenido de la clase',
		0,
		'http://curso2/clase2',
		2,
		2
	),
	(
		'Clase 1 - Presentación',
		'Presentación del profesor',
		'Contenido de la clase',
		0,
		'http://curso3/clase1',
		1,
		3
	),
	(
		'Clase 2 - Las estructuras societarias',
		'Muchas es mejor!!',
		'Contenido de la clase',
		0,
		'http://curso3/clase2',
		2,
		3
	),
	(
		'Clase 1 - Tu nombre te define',
		'Tu nombre, tu marca',
		'Contenido de la clase',
		0,
		'http://curso4/clase1',
		1,
		4
	),
	(
		'Clase 2 - Copyright en Derecho Consuetudinario',
		'En common law se hace publico',
		'Contenido de la clase',
		0,
		'http://curso4/clase2',
		2,
		4
	);
INSERT INTO plan (nombre_plan, precio_plan)
VALUES ('Absolute', 1500.00),
	('Student', 100.00);
INSERT INTO usuario (
		fecha_registro_usuario,
		foto_usuario,
		presentacion,
		nombre_usuario,
		roll_usuario,
		plan_usuario,
		is_enabled,
		account_no_expired,
		account_no_locked,
		credentials_no_expired
	)
VALUES (
		'2024-01-01',
		ARRAY ['https://localhost:8080/usuario/fotos/roble_texto.svg'],
		'El creador de la Autarquía de la Gole, y el creador de esta plataforma.',
		'Matt I de la Gole®',
		'ADMIN',
		NULL,
		TRUE,
		TRUE,
		TRUE,
		TRUE
	),
	(
		'2024-01-01',
		ARRAY ['https://localhost:8080/usuario/fotos/sello-ex-libris-1.webp'],
		'El mayor activista de la REML del Mundo!!! Creador del primer Pasaporte Oficial REML.',
		'Ra Amon de la REML®',
		'PROF',
		NULL,
		TRUE,
		TRUE,
		TRUE,
		TRUE
	),
	(
		'2024-01-01',
		ARRAY ['https://localhost:8080/usuario/fotos/Logo-REML.webp'],
		'Uno de los más antiguos Soberanos de la REML, con muchos años de enseñanza a sus espaldas.',
		'Rodri KMTR',
		'PROF',
		NULL,
		TRUE,
		TRUE,
		TRUE,
		TRUE
	),
	(
		'2024-01-01',
		ARRAY ['https://localhost:8080/usuario/fotos/Logo_LJS_morado.webp'],
		'Grupo de Hombre y Mujeres Libres y Soberanos encentrados en crear técnicas Soberanas para cada necesidad.',
		'El Equipo De L.J.S.',
		'PROF',
		NULL,
		TRUE,
		TRUE,
		TRUE,
		TRUE
	),
	(
		'2024-01-01',
		ARRAY ['https://localhost:8080/usuario/fotos/jaime.webp'],
		'Aprendiz Soberano, con muchas ganar de Libertad.',
		'Jaime',
		'USER',
		1,
		TRUE,
		TRUE,
		TRUE,
		TRUE
	);
INSERT INTO login (id_usuario, correo_electronico, PASSWORD)
VALUES (
		1,
		'matt@prueba.com',
		'$2a$10$9Ii165AsuCR6YeKaJ/FHN.RyTvt1.FdjaeN/6s5ijccjT6hNE1ATW'
	),
	(2, 'ra@prueba.com', 234567),
	(3, 'rodri@prueba.com', 987654),
	(4, 'equipoljs@prueba.com', 876543),
	(5, 'jaime@prueba.com', 345678);
INSERT INTO curso_profesor (id_curso, id_usuario)
VALUES (1, 3),
	(2, 2),
	(3, 1),
	(4, 4);
INSERT INTO usuario_curso (id_usuario, id_curso)
VALUES (5, 2);
INSERT INTO cursos_plan (id_curso, id_plan)
VALUES (1, 2),
	(2, 1);