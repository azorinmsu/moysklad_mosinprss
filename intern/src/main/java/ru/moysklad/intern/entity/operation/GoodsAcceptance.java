package ru.moysklad.intern.entity.operation;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Приемка. Приходует купленный товар на склад.
 */
@Entity
@Table(name = "goods_acceptance")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("GoodsAcceptance")
public class GoodsAcceptance extends DocumentsWithPositions {

    @Override
    public DocumentsType getDocumentsType() {
        return DocumentsType.GOODS_ACCEPTANCE;
    }
}
