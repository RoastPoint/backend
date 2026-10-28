package ru.roastpoint;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** One-time migration of legacy demonstration markers; never resets existing hashes. */
@Component
public class DemoPasswords implements ApplicationRunner {
 private final JdbcClient db;
 private final PasswordEncoder encoder;
 private final String password;
 public DemoPasswords(JdbcClient db, PasswordEncoder encoder, @Value("${DEMO_PASSWORD:}") String password) {
  this.db=db; this.encoder=encoder; this.password=password;
 }
 @Override @Transactional public void run(ApplicationArguments args) {
  if(!password.isEmpty() && (password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72))
   throw new IllegalArgumentException("DEMO_PASSWORD must contain at least 12 characters and at most 72 UTF-8 bytes");
  // Without an explicit initial password, demo accounts receive unknown random passwords.
  for(int id:db.sql("SELECT id FROM users WHERE password_hash IN ('DEMO_NOT_A_REAL_HASH',:seed) FOR UPDATE")
   .param("seed","$2a$12$bcW4Y2fzGMaZEu3ZK1EAE.LH8Nf8lCNrI3jn0PpFDV81349cmMiLK").query(Integer.class).list()) {
   db.sql("UPDATE users SET password_hash=:hash WHERE id=:id")
    .param("hash",encoder.encode(password.isEmpty()?UUID.randomUUID().toString():password)).param("id",id).update();
  }
 }
}
