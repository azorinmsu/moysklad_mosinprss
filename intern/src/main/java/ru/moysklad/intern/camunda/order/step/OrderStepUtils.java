package ru.moysklad.intern.camunda.order.step;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import ru.moysklad.intern.camunda.ProcessErrors;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.remap_1_2.entities.MetaEntity;
import ru.moysklad.remap_1_2.entities.products.markers.ProductMarker;

import java.util.UUID;

public final class OrderStepUtils {

    private OrderStepUtils() {
    }

    public static UUID extractOrderId(DelegateExecution execution) {
        String orderIdStr = (String) execution.getVariable(ProcessVariables.ORDER_ID);
        if (orderIdStr == null || orderIdStr.isBlank()) {
            orderIdStr = execution.getBusinessKey();
        }
        if (orderIdStr == null || orderIdStr.isBlank()) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Идентификатор заказа (orderId) не задан");
        }
        try {
            return UUID.fromString(orderIdStr.trim());
        } catch (IllegalArgumentException e) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Некорректный UUID заказа: " + orderIdStr);
        }
    }

    public static String extractProductId(ProductMarker assortment) {
        if (assortment instanceof MetaEntity metaEntity) {
            if (metaEntity.getId() != null && !metaEntity.getId().isBlank()) {
                return metaEntity.getId();
            }
            if (metaEntity.getMeta() != null && metaEntity.getMeta().getHref() != null) {
                String href = metaEntity.getMeta().getHref();
                return href.substring(href.lastIndexOf('/') + 1);
            }
        }
        return null;
    }

    public static String extractIdFromHref(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        int lastSlash = href.lastIndexOf('/');
        return lastSlash >= 0 ? href.substring(lastSlash + 1) : href;
    }
}
