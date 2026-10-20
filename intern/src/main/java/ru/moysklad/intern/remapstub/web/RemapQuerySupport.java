package ru.moysklad.intern.remapstub.web;

final class RemapQuerySupport {

    private RemapQuerySupport() {
    }

    static boolean expandPositions(String expand) {
        if (expand == null || expand.isBlank()) {
            return false;
        }
        for (String part : expand.split(",")) {
            if ("positions".equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    static boolean includeStock(String fields) {
        if (fields == null || fields.isBlank()) {
            return false;
        }
        for (String part : fields.split(",")) {
            if ("stock".equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }
}
