package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Операция над документом — общий интерфейс компонента и декораторов.
 * Базовая реализация — обработчик проведения ({@code AbstractDocumentProcessor}).
 */
@FunctionalInterface
public interface DocumentOperation {

    void execute(ErpDocument document);
}
