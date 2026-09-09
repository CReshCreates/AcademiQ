package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.Models.MyUserDetails;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        try{
            return new MyUserDetails(userRepository.findByEmail(email));
        }catch(Exception e){
            throw new UsernameNotFoundException(e.getMessage());
        }
    }
}
