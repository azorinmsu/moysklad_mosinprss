package ru.moysklad.intern.utils.auth;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.config.Configuration;

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
                new Configuration().credentialsToBase64()
        )) {
            return Status.AUTH_INCORRECT;
        } else {
            return Status.OK;
        }
    }
}
