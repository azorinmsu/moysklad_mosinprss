package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Реестр фабрик по типу документа. <b>Готовая инфраструктура.</b>
 *
 * <p>Это не Factory Method и не Abstract Factory, а вспомогательный поиск (по сути Simple Factory):
 * он лишь выбирает зарегистрированную фабрику. Новые типы регистрируются списком фабрик — без {@code switch}.
 */
public final class DocumentFactoryRegistry {

    private final Map<DocumentsType, DocumentFactory> factories = new EnumMap<>(DocumentsType.class);

    public DocumentFactoryRegistry(Collection<? extends DocumentFactory> factories) {
        Objects.requireNonNull(factories, "factories");
        for (DocumentFactory factory : factories) {
            Objects.requireNonNull(factory, "factory");
            DocumentFactory previous = this.factories.putIfAbsent(factory.documentType(), factory);
            if (previous != null) {
                throw new IllegalArgumentException("Для типа " + factory.documentType() + " уже зарегистрирована фабрика "
                        + previous.getClass().getSimpleName());
            }
        }
    }

    public DocumentFactory forType(DocumentsType type) {
        DocumentFactory factory = factories.get(type);
        if (factory == null) {
            throw new DocumentProcessingException(DocumentErrorCode.UNSUPPORTED_DOCUMENT_TYPE,
                    "Тип документа не поддерживается: " + type);
        }
        return factory;
    }

    public Set<DocumentsType> supportedTypes() {
        return Set.copyOf(factories.keySet());
    }

    public Collection<DocumentFactory> factories() {
        return List.copyOf(factories.values());
    }
}
