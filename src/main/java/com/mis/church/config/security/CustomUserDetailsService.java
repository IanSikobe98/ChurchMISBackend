package com.mis.church.config.security;




import com.mis.church.entity.User;
import com.mis.church.service.DatabaseService;
import com.mis.church.util.ConstantUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final DatabaseService databaseService;
    private final ConstantUtil constantUtil;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = databaseService.getUserByUsername(username,constantUtil);
        if(user == null) throw new UsernameNotFoundException("User not Found");
        return new SecurityUser(user,constantUtil);
    }
}
