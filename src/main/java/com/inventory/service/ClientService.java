package com.inventory.service;

import com.inventory.dto.ClientPasswordRequest;
import com.inventory.entity.Client;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UtilityService utilityService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void setClientPassword(ClientPasswordRequest request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        if (currentUser.getClient() == null) {
            throw new ValidationException("User is not associated with any client", HttpStatus.BAD_REQUEST);
        }

        Client client = currentUser.getClient();
        client.setPassword(passwordEncoder.encode(request.getPassword()));
        clientRepository.save(client);
    }

    public boolean validateClientPassword(ClientPasswordRequest request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        if (currentUser.getClient() == null) {
            throw new ValidationException("User is not associated with any client", HttpStatus.BAD_REQUEST);
        }

        Client client = currentUser.getClient();
        
        // Check if locked
        if (client.getLockTime() != null) {
            if (client.getLockTime().isAfter(java.time.OffsetDateTime.now())) {
                 throw new ValidationException("Account is locked. Please try again after 10 minutes", HttpStatus.FORBIDDEN);
            } else {
                // Unlock if time passed
                client.setLockTime(null);
                client.setFailPasswordCount(0);
                clientRepository.save(client);
            }
        }

        if (client.getPassword() == null) {
            throw new ValidationException("Client password not set", HttpStatus.BAD_REQUEST);
        }

        boolean isMatch = passwordEncoder.matches(request.getPassword(), client.getPassword());
        
        if (isMatch) {
            // Reset counters on success
            if (client.getFailPasswordCount() > 0 || client.getLockTime() != null) {
                client.setFailPasswordCount(0);
                client.setLockTime(null);
                clientRepository.save(client);
            }
            return true;
        } else {
            // Increment failure count
            int currentCount = client.getFailPasswordCount() == null ? 0 : client.getFailPasswordCount();
            int newCount = currentCount + 1;
            client.setFailPasswordCount(newCount);
            
            if (newCount >= 3) {
                client.setLockTime(java.time.OffsetDateTime.now().plusMinutes(10));
            }
            clientRepository.save(client);
            return false;
        }
    }
}
