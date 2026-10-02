package ru.moysklad.intern.services.versioning;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 *  the purpose of this class to provide auth and
 *  <i>"/api"</i>-prefix before any other endpoint <b>URL</b>
 */

@RequestMapping("/api")
abstract public class BaseService {
}
