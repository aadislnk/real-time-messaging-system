package com.aadi.sandesh.repository;

import com.aadi.sandesh.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

}
