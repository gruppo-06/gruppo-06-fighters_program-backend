package it.fightersprogram.economy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.identity.UserDirectory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCreditService {

    private final CreditService creditService;
    private final UserDirectory userDirectory;

    @Transactional
    public AdminCreditResult grant(String nickname, long adminId, String idempotencyKey, AdminCreditRequest request) {
        if (request.amount() == 0) {
            throw new BadRequestException("The credit amount must not be zero");
        }
        long userId = userDirectory.findIdByNickname(nickname)
                .orElseThrow(() -> new NotFoundException("User not found: " + nickname));
        String key = "admin-grant:" + idempotencyKey;
        String reference = request.reason() + " (by " + userDirectory.nicknameOf(adminId) + ")";
        CreditMovement movement = request.amount() > 0
                ? creditService.credit(userId, request.amount(), CreditType.ADMIN_GRANT, key, reference)
                : creditService.debit(userId, -request.amount(), CreditType.ADMIN_GRANT, key, reference);
        return new AdminCreditResult(nickname, creditService.balance(userId), movement);
    }
}
