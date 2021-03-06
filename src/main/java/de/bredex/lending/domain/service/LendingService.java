package de.bredex.lending.domain.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import de.bredex.lending.domain.model.Lending;
import de.bredex.lending.domain.spi.AccountServiceProvider;
import de.bredex.lending.domain.spi.InventoryServiceProvider;
import de.bredex.lending.domain.spi.LendingEntity;
import de.bredex.lending.domain.spi.LendingRepository;

@Service
public final class LendingService {

    private final AccountServiceProvider accountService;
    private final InventoryServiceProvider inventoryService;
    private final LendingRepository repository;

    public LendingService(final AccountServiceProvider accountService, final InventoryServiceProvider inventoryService,
                          final LendingRepository repository) {
        this.accountService = accountService;
        this.inventoryService = inventoryService;
        this.repository = repository;
    }

    public final Lending borrow(final String accountNumber, String isbn) {
        if (!accountService.accountExists(accountNumber)) {
            throw new IllegalArgumentException("Account with number '" + accountNumber + "' does not exists.");
        }

        if (!inventoryService.bookExists(isbn)) {
            throw new IllegalArgumentException("Book with ISBN '" + isbn + "' does not exists.");
        }

        final LendingEntity savedLending = repository
            .save(new LendingEntity(accountNumber, isbn, LocalDate.now().plus(4, ChronoUnit.WEEKS)));
        return new Lending(savedLending.getAccountNumber(), savedLending.getIsbn(), savedLending.getReturnDate());
    }

    public final List<Lending> getLendings(final String accountNumber) {
        final List<LendingEntity> lendings = repository.findAllByAccountNumber(accountNumber);
        return Collections.unmodifiableList(lendings.stream()
            .map(lending -> new Lending(lending.getAccountNumber(), lending.getIsbn(), lending.getReturnDate()))
            .collect(Collectors.toList()));
    }

    public final void deleteLending(final String accountNumber, final String isbn) {
        final Optional<LendingEntity> lending = repository.findByAccountNumberAndIsbn(accountNumber, isbn);

        if (lending.isPresent()) {
            repository.delete(lending.get());
        } else {
            throw new IllegalArgumentException(
                String.format("No lending of ISBN '%s' found for account '%s'.", isbn, accountNumber));
        }
    }
}
