package ru.roastpoint;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUsers implements UserDetailsService {
 private final JdbcClient db;
 public DatabaseUsers(JdbcClient db) { this.db=db; }
 @Override public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
  return db.sql("SELECT login,password_hash,role FROM users WHERE login=:login")
   .param("login",login)
   .query((rs,n) -> User.withUsername(rs.getString("login")).password(rs.getString("password_hash"))
    .roles(rs.getString("role")).build()).optional()
   .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль"));
 }
}
