package ru.moysklad.intern.util.auth;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class Authorization {
    // simply I will send auth check here
    // also temp method
    public static Status checkAuth(String encodeString) {
        // Возможно, будет полезно сделать не энкод-строку, а
        // проверку вплоть до логина и пароля для выдачи соответствующих
        // результатов - например, IncorrectPassword и так далее
        if (!Objects.equals(
                encodeString,
                "bnVsbDpudWxs"
        )) {
            return Status.AUTH_INCORRECT;
        } else {
            return Status.OK;
        }
    }
}
