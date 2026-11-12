package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Собранная цепочка проверок. Снаружи выглядит как один {@link DocumentValidator}, поэтому обработчик
 * не знает, сколько правил и в каком порядке проверяется.
 *
 * <p>Расширение — новый класс-звено и новая сборка через {@link #of}, без правки существующих звеньев.
 * Экземпляры звеньев не переиспользуйте между разными цепочками: {@code of} перелинковывает их.
 */
public final class DocumentValidationChain implements DocumentValidator {

    private final List<AbstractDocumentValidator> links;

    private DocumentValidationChain(List<AbstractDocumentValidator> links) {
        this.links = links;
    }

    /** Связывает звенья в указанном порядке. <b>Готовая инфраструктура.</b> */
    public static DocumentValidationChain of(AbstractDocumentValidator... validators) {
        Objects.requireNonNull(validators, "validators");
        if (validators.length == 0) {
            throw new IllegalArgumentException("Цепочка должна содержать хотя бы одно звено");
        }
        Map<AbstractDocumentValidator, Boolean> seen = new IdentityHashMap<>();
        for (AbstractDocumentValidator validator : validators) {
            Objects.requireNonNull(validator, "validator");
            if (seen.put(validator, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("Звено добавлено в цепочку дважды: " + validator);
            }
        }
        for (int i = 0; i < validators.length - 1; i++) {
            validators[i].linkWith(validators[i + 1]);
        }
        validators[validators.length - 1].linkWith(null);
        return new DocumentValidationChain(List.of(validators));
    }

    /**
     * Стандартная цепочка проверок перед проведением.
     */
    public static DocumentValidationChain defaultChain() {
        return of(
                new PositionValidator(),
                new QuantityValidator(),
                new WarehouseValidator(),
                new StockAvailabilityValidator()
        );
    }

    @Override
    public void validate(ErpDocument document, ValidationContext context) {
        links.getFirst().validate(document, context);
    }

    /** Звенья в порядке выполнения. */
    public List<AbstractDocumentValidator> links() {
        return links;
    }
}
