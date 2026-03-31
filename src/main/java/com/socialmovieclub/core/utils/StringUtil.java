package com.socialmovieclub.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // Kimse bu sınıftan nesne üretmesin
public class StringUtil {

    /**
     * Metni belirli bir uzunlukta keser ve sonuna "..." ekler.
     * Metni belirtilen uzunluğa göre güvenli bir şekilde keser.
     * @param text Kesilecek metin
     * @param maxLength Maksimum karakter sayısı (Üç nokta dahil)
     * @return Kısaltılmış metin (Örn: "Çok uzun bir me...")
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        //Negatif deger kontrolu ve guvenli kesme
        int end = Math.max(0, maxLength - 3);
        return text.substring(0, end) + "...";
    }
}
