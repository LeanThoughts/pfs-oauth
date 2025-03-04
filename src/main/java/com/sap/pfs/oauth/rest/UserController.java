package com.sap.pfs.oauth.rest;

import com.sap.pfs.oauth.auth.*;
import com.sap.pfs.oauth.resource.SignupResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.List;

@Slf4j
@ApiController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    private final PasswordHistoryRepository passwordHistoryRepository;

    private final LoginAttemptService loginAttemptService;

    @PutMapping("/password/modify")
    public ResponseEntity changePassword(@RequestBody SignupResource resource, Principal principal) {
        User user = userRepository.findByEmail(resource.getEmail());

        //Invalidate Attempts Cache
        loginAttemptService.invalidateAttemptsCache(resource.getEmail());

        if (!user.getPassword().equals(resource.getPassword())) {
            if (ValidateForHistory(user.getEmail(), resource.getPassword())) {
                user.modifyPassword(resource.getPassword());
                user = userRepository.save(user);

                // In case the user is blocked for wrong entry of passwords, reset the attemptsCache


                PasswordHistory passwordHistory = new PasswordHistory(user.getEmail(), user.getPassword());
                passwordHistory = passwordHistoryRepository.save(passwordHistory);
            } else {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(new RuntimeException("Password cannot " +
                        "be the same as 3 previously entered passwords"));
            }
        }
        return ResponseEntity.ok().build();
    }

    /*
        ValidateForHistory()
        Return false if password matches any of the 3 previously entered passwords.
     */
    private boolean ValidateForHistory(String email, String password) {
        List<PasswordHistory> historyList = passwordHistoryRepository.findTop3ByEmailOrderByCreatedDesc(email);
        for (PasswordHistory passwordHistory : historyList) {
            if (passwordHistory.getPassword() != null) {
                if (passwordHistory.getPassword().equals(password))
                    return false;
            }
            else
                return true;
        }
        return true;
    }

    @PutMapping("/password/reset")
    public ResponseEntity resetPassword(@RequestBody SignupResource resource) {
        User user = userRepository.findByEmail(resource.getEmail());
        user.modifyPassword(resource.getPassword());
        user = userRepository.save(user);

        // In case the user is blocked for wrong entry of passwords, reset the attemptsCache
        loginAttemptService.invalidateAttemptsCache(resource.getEmail());

        log.info("Password changed for {}",user);
        return ResponseEntity.ok().build();
    }
}