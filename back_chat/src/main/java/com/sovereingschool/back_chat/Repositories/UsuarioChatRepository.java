package com.sovereingschool.back_chat.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_chat.Models.UsuarioChat;

@Repository
public interface UsuarioChatRepository extends MongoRepository<UsuarioChat, Long> {

}
