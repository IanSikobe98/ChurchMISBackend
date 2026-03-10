package com.mis.church.config.security;




import com.mis.church.service.DatabaseService;
import com.mis.church.util.ConstantUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class UserManagementConfig {
    private final DatabaseService databaseService;
    private final ConstantUtil constantUtil;

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService(databaseService, constantUtil);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
