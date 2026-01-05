package com.socialmovieclub.core.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageHelper {

    private final MessageSource messageSource;

    /**
     * Verilen key'e göre, o anki sistem dilinde mesaj döner.
     * @param key messages_tr.properties içindeki anahtar
     * @param args mesajın içine gömülecek dinamik veriler (film adı vb.)
     */
    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}