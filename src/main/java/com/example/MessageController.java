package com.example;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
	private final List<Message> messages = new CopyOnWriteArrayList<>();

	@GetMapping(path = "/messages")
	List<Message> getMessages() {
		return this.messages;
	}

	@PostMapping(path = "/messages")
	Message postMessages(@RequestBody String text, @AuthenticationPrincipal Jwt jwt) {
		Message message = new Message(text, jwt.getSubject());
		this.messages.add(message);
		return message;
	}

	record Message(String text, String username) {
	}
}
