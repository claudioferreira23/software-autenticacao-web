package br.ufg.autenticacao.infra;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "email", nullable = false, length = 254, unique = true)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	protected UsuarioEntity() {
	}

	public UsuarioEntity(String nome, String email, String senhaHash) {
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
	}

	public UUID getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	@Override
	public String toString() {
		return "UsuarioEntity[id=" + id + ", nome=" + nome + ", email=" + email + "]";
	}

}
