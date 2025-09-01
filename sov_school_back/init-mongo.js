// Inicializa el replica set
try {
	rs.initiate({ _id: "rs0", version: 1, members: [{ _id: 0, host: "sovschool-mongo:27017" }] });
	print("Replica set iniciado");
} catch (e) {
	print("Replica set ya iniciado o error: " + e.message);
}

// Base de datos SovSchoolChat
db = db.getSiblingDB("SovSchoolChat");
db.courses_chat.insertOne({ createdAt: new Date() });
db.messages_chat.insertOne({ createdAt: new Date() });
db.users_chat.insertOne({ createdAt: new Date() });

// Base de datos SovSchoolStream
db = db.getSiblingDB("SovSchoolStream");
db.presets.insertOne({ createdAt: new Date() });
db.user_courses.insertOne({ createdAt: new Date() });
