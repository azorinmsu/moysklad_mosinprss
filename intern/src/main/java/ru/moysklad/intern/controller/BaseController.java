package ru.moysklad.intern.controller;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 *  the purpose of this class to provide
 *  <i>"/api"</i>-prefix before any other endpoint <b>URL</b>
 */

@RequestMapping("/api")
abstract public class BaseController {
}
