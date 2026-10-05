package ru.moysklad.intern.repo.meta;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.meta.Currency;

import java.util.UUID;

public interface CurrencyRepository extends JpaRepository<Currency, UUID> {
    Currency findByName(String name);
}
